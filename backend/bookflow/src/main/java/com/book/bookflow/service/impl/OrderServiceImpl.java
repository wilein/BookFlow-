package com.book.bookflow.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.common.pay.WechatPayClient;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.CartItem;
import com.book.bookflow.entity.ContentReport;
import com.book.bookflow.entity.Notification;
import com.book.bookflow.entity.OrderIssue;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserAddress;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.CartItemMapper;
import com.book.bookflow.mapper.ContentReportMapper;
import com.book.bookflow.mapper.NotificationMapper;
import com.book.bookflow.mapper.OrderIssueMapper;
import com.book.bookflow.mapper.UserAddressMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.service.OrderService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_AVATAR = "/static/logo.png";
    private static final String DEFAULT_NICKNAME = "书友";

    private final BookOrderMapper bookOrderMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;
    private final CartItemMapper cartItemMapper;
    private final OrderIssueMapper orderIssueMapper;
    private final ContentReportMapper contentReportMapper;
    private final NotificationMapper notificationMapper;
    private final WechatPayClient wechatPayClient;

    public OrderServiceImpl(BookOrderMapper bookOrderMapper,
                            BookMapper bookMapper,
                            UserMapper userMapper,
                            UserAddressMapper userAddressMapper,
                            CartItemMapper cartItemMapper,
                            OrderIssueMapper orderIssueMapper,
                            ContentReportMapper contentReportMapper,
                            NotificationMapper notificationMapper,
                            WechatPayClient wechatPayClient) {
        this.bookOrderMapper = bookOrderMapper;
        this.bookMapper = bookMapper;
        this.userMapper = userMapper;
        this.userAddressMapper = userAddressMapper;
        this.cartItemMapper = cartItemMapper;
        this.orderIssueMapper = orderIssueMapper;
        this.contentReportMapper = contentReportMapper;
        this.notificationMapper = notificationMapper;
        this.wechatPayClient = wechatPayClient;
    }

    @Override
    @Transactional
    public Map<String, Object> createOrder(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long bookId = asLong(payload.get("bookId"));
        if (bookId == null) {
            throw new CustomerException("400", "书籍参数错误");
        }
        UserAddress address = resolveCheckoutAddress(currentUserId, asLong(payload.get("addressId")));
        String buyerMessage = defaultString(payload.get("buyerMessage"), "");
        return createOrderForBook(currentUserId, bookId, address, buyerMessage);
    }

    @Override
    @Transactional
    public Map<String, Object> checkout(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        List<Map<String, Object>> items = asMapList(payload.get("items"));
        if (items.isEmpty()) {
            throw new CustomerException("400", "请选择要结算的书籍");
        }
        UserAddress address = resolveCheckoutAddress(currentUserId, asLong(payload.get("addressId")));
        String buyerMessage = defaultString(payload.get("buyerMessage"), "");

        List<Map<String, Object>> orders = new ArrayList<>();
        List<Long> orderIds = new ArrayList<>();
        List<Long> cartItemIds = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Long bookId = asLong(item.get("bookId"));
            Long cartItemId = asLong(item.get("cartItemId"));
            if (bookId == null && cartItemId != null) {
                CartItem cartItem = requireCartItem(cartItemId, currentUserId);
                bookId = cartItem.getBookId();
            }
            Map<String, Object> order = createOrderForBook(currentUserId, bookId, address, buyerMessage);
            orders.add(order);
            orderIds.add(asLong(order.get("id")));
            if (cartItemId != null) {
                cartItemIds.add(cartItemId);
            }
        }
        removeCheckedCartItems(currentUserId, cartItemIds);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orders", orders);
        result.put("orderIds", orderIds);
        return result;
    }

    @Override
    public Map<String, Object> cancelOrder(Long orderId) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireBuyerOrder(orderId, currentUserId);
        if (!Objects.equals(order.getStatus(), 1)) {
            throw new CustomerException("400", "当前订单不可取消");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(5);
        order.setCloseTime(now);
        order.setUpdateTime(now);
        bookOrderMapper.update(order);

        Book book = order.getBookId() == null ? null : bookMapper.selectOneById(order.getBookId());
        if (book != null && !isDeleted(book.getIsDeleted()) && Objects.equals(book.getStatus(), 2)) {
            book.setStatus(1);
            book.setUpdateTime(now);
            bookMapper.update(book);
        }
        createNotification(order.getSellerId(), "order", "订单已取消", "订单 " + defaultString(order.getOrderNo(), "") + " 已取消", "/pages/my/orders?role=seller&status=5");
        return toOrderItem(order, currentUserId);
    }

    @Override
    public Map<String, Object> payOrder(Long orderId) {
        return createPaymentPrepay(orderId);
    }

    @Override
    public Map<String, Object> createPaymentPrepay(Long orderId) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireBuyerOrder(orderId, currentUserId);
        if (!Objects.equals(order.getStatus(), 1)) {
            throw new CustomerException("400", "当前订单不可支付");
        }
        Book book = order.getBookId() == null ? null : requireBook(order.getBookId());
        User buyer = currentUserId == null ? null : userMapper.selectOneById(currentUserId);
        return wechatPayClient.createPrepay(order, buyer, book);
    }

    @Override
    @Transactional
    public Map<String, Object> payMock(Long orderId) {
        if (!wechatPayClient.isMockMode()) {
            throw new CustomerException("403", "模拟支付仅开发支付模式可用");
        }
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireBuyerOrder(orderId, currentUserId);
        return completePayment(order, currentUserId, 0, LocalDateTime.now());
    }

    @Override
    public Map<String, Object> getPayStatus(Long orderId) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireParticipantOrder(orderId, currentUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", defaultString(order.getOrderNo(), ""));
        result.put("status", order.getStatus());
        result.put("statusLabel", mapOrderStatus(order.getStatus()));
        result.put("paid", List.of(2, 3, 4).contains(order.getStatus()));
        result.put("paymentTime", formatTime(order.getPaymentTime()));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> handleWechatPayNotify(Map<String, String> headers, String body) {
        JSONObject notify = wechatPayClient.parseNotify(
            header(headers, "Wechatpay-Serial"),
            header(headers, "Wechatpay-Timestamp"),
            header(headers, "Wechatpay-Nonce"),
            header(headers, "Wechatpay-Signature"),
            body
        );
        if (!"SUCCESS".equalsIgnoreCase(defaultString(notify.getString("trade_state"), ""))) {
            return wechatNotifySuccess();
        }
        String outTradeNo = defaultString(notify.getString("out_trade_no"), "");
        if (outTradeNo.isBlank()) {
            throw new CustomerException("400", "微信支付回调缺少订单号");
        }
        if (!defaultString(wechatPayClient.getMchId(), "").equals(defaultString(notify.getString("mchid"), ""))) {
            throw new CustomerException("400", "微信支付商户号不匹配");
        }
        if (!defaultString(wechatPayClient.getAppid(), "").equals(defaultString(notify.getString("appid"), ""))) {
            throw new CustomerException("400", "微信支付应用号不匹配");
        }

        BookOrder order = bookOrderMapper.selectOneByQuery(
            QueryWrapper.create().where("order_no = ?", outTradeNo).and("is_deleted = 0").limit(1)
        );
        if (order == null) {
            throw new CustomerException("404", "支付订单不存在");
        }
        JSONObject amount = notify.getJSONObject("amount");
        int paidAmount = amount == null ? 0 : (amount.getInteger("payer_total") == null ? amount.getIntValue("total") : amount.getIntValue("payer_total"));
        if (paidAmount != wechatPayClient.expectedAmountFen(order)) {
            throw new CustomerException("400", "微信支付金额不匹配");
        }
        completePayment(order, order.getBuyerId(), 1, parseWechatPayTime(notify.getString("success_time")));
        return wechatNotifySuccess();
    }

    @Override
    public Map<String, Object> confirmReceipt(Long orderId) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireBuyerOrder(orderId, currentUserId);
        if (!Objects.equals(order.getStatus(), 3)) {
            throw new CustomerException("400", "当前订单不可确认收货");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(4);
        order.setReceiveTime(now);
        order.setUpdateTime(now);
        bookOrderMapper.update(order);

        Book book = order.getBookId() == null ? null : bookMapper.selectOneById(order.getBookId());
        if (book != null && !isDeleted(book.getIsDeleted())) {
            book.setStatus(3);
            book.setUpdateTime(now);
            bookMapper.update(book);
        }
        createNotification(order.getSellerId(), "order", "订单已完成", "订单 " + defaultString(order.getOrderNo(), "") + " 已确认收货", "/pages/my/orders?role=seller&status=4");
        return toOrderItem(order, currentUserId);
    }

    @Override
    public Map<String, Object> getOrderDetail(Long orderId) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireParticipantOrder(orderId, currentUserId);
        return toOrderItem(order, currentUserId);
    }

    @Override
    public Map<String, Object> shipOrder(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireSellerOrder(asLong(payload.get("orderId")), currentUserId);
        if (!Objects.equals(order.getStatus(), 2)) {
            throw new CustomerException("400", "当前订单不可发货");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(3);
        order.setDeliveryTime(now);
        order.setUpdateTime(now);
        bookOrderMapper.update(order);
        createNotification(order.getBuyerId(), "order", "卖家已发货", "订单 " + defaultString(order.getOrderNo(), "") + " 已发货", "/pages/my/orders?role=buyer&status=3");
        return toOrderItem(order, currentUserId);
    }

    @Override
    public Map<String, Object> createIssue(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireParticipantOrder(asLong(payload.get("orderId")), currentUserId);
        int type = mapIssueType(defaultString(payload.get("type"), "question"));
        String content = defaultString(payload.get("content"), "");
        if (content.isBlank()) {
            throw new CustomerException("400", "请填写问题内容");
        }
        LocalDateTime now = LocalDateTime.now();
        OrderIssue issue = OrderIssue.builder()
            .orderId(order.getId())
            .userId(currentUserId)
            .type(type)
            .content(content)
            .status(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        orderIssueMapper.insert(issue);

        if (type != 1 && !Objects.equals(order.getStatus(), 5) && !Objects.equals(order.getStatus(), 4)) {
            order.setStatus(6);
            order.setUpdateTime(now);
            bookOrderMapper.update(order);
        }

        Long notifyUserId = currentUserId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();
        createNotification(notifyUserId, "order_issue", "订单有新的问题反馈", "订单 " + defaultString(order.getOrderNo(), "") + " 收到新的" + mapIssueTypeLabel(type), "/pages/my/order-detail?orderId=" + order.getId());
        Map<String, Object> result = toIssueItem(issue, currentUserId, order);
        result.put("orderStatus", order.getStatus());
        return result;
    }

    @Override
    public List<Map<String, Object>> getIssueList(Long orderId) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireParticipantOrder(orderId, currentUserId);
        List<OrderIssue> issues = orderIssueMapper.selectListByQuery(
            QueryWrapper.create()
                .where("order_id = ?", orderId)
                .and("is_deleted = 0")
                .orderBy("id desc")
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrderIssue issue : issues) {
            result.add(toIssueItem(issue, currentUserId, order));
        }
        return result;
    }

    @Override
    public Map<String, Object> replyIssue(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long issueId = asLong(payload.get("issueId"));
        String replyContent = defaultString(payload.get("replyContent"), "");
        if (issueId == null) {
            throw new CustomerException("400", "问题参数错误");
        }
        if (replyContent.isBlank()) {
            throw new CustomerException("400", "请填写回复内容");
        }
        OrderIssue issue = orderIssueMapper.selectOneById(issueId);
        if (issue == null || isDeleted(issue.getIsDeleted())) {
            throw new CustomerException("404", "问题记录不存在");
        }
        BookOrder order = requireParticipantOrder(issue.getOrderId(), currentUserId);
        if (currentUserId.equals(issue.getUserId())) {
            throw new CustomerException("400", "不能回复自己发起的问题");
        }
        LocalDateTime now = LocalDateTime.now();
        issue.setReplyContent(replyContent);
        issue.setReplyUserId(currentUserId);
        issue.setReplyTime(now);
        issue.setStatus(1);
        issue.setUpdateTime(now);
        orderIssueMapper.update(issue);

        createNotification(issue.getUserId(), "order_issue", "订单问题已回复", "订单 " + defaultString(order.getOrderNo(), "") + " 的问题已收到回复", "/pages/my/order-detail?orderId=" + order.getId());
        return toIssueItem(issue, currentUserId, order);
    }

    @Override
    public Map<String, Object> reportOrder(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        BookOrder order = requireParticipantOrder(asLong(payload.get("orderId")), currentUserId);
        String reasonType = defaultString(payload.get("reasonType"), "其他");
        String content = defaultString(payload.get("content"), "");
        if (content.isBlank()) {
            throw new CustomerException("400", "请填写举报说明");
        }
        LocalDateTime now = LocalDateTime.now();
        ContentReport report = ContentReport.builder()
            .userId(currentUserId)
            .targetType("order")
            .targetId(order.getId())
            .reasonType(reasonType)
            .content(content)
            .status(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        contentReportMapper.insert(report);
        createNotification(currentUserId, "report", "订单举报已提交", "我们已收到你的订单举报，将在后续处理", "/pages/my/order-detail?orderId=" + order.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", report.getId());
        result.put("targetId", order.getId());
        result.put("targetType", "order");
        result.put("status", 0);
        return result;
    }

    private Map<String, Object> completePayment(BookOrder sourceOrder, Long currentUserId, int paymentMethod, LocalDateTime paymentTime) {
        BookOrder order = bookOrderMapper.selectOneById(sourceOrder.getId());
        if (order == null || isDeleted(order.getIsDeleted())) {
            throw new CustomerException("404", "订单不存在");
        }
        if (List.of(2, 3, 4).contains(order.getStatus())) {
            return toOrderItem(order, currentUserId == null ? order.getBuyerId() : currentUserId);
        }
        if (!Objects.equals(order.getStatus(), 1)) {
            throw new CustomerException("400", "当前订单不可支付");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(2);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(paymentTime == null ? now : paymentTime);
        order.setUpdateTime(now);
        bookOrderMapper.update(order);

        Book book = order.getBookId() == null ? null : bookMapper.selectOneById(order.getBookId());
        if (book != null && !isDeleted(book.getIsDeleted()) && Objects.equals(book.getStatus(), 1)) {
            book.setStatus(2);
            book.setUpdateTime(now);
            bookMapper.update(book);
        }
        createNotification(order.getSellerId(), "order", "买家已付款", "订单 " + defaultString(order.getOrderNo(), "") + " 等待发货", "/pages/my/orders?role=seller&status=2");
        return toOrderItem(order, currentUserId == null ? order.getBuyerId() : currentUserId);
    }

    private Map<String, Object> wechatNotifySuccess() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", "SUCCESS");
        result.put("message", "成功");
        return result;
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return defaultString(entry.getValue(), "");
            }
        }
        return "";
    }

    private LocalDateTime parseWechatPayTime(String value) {
        String text = defaultString(value, "");
        if (text.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            return LocalDateTime.now();
        }
    }

    private Map<String, Object> createOrderForBook(Long currentUserId, Long bookId, UserAddress address, String buyerMessage) {
        Book book = requireBook(bookId);
        if (!Objects.equals(book.getStatus(), 1)) {
            throw new CustomerException("400", "该书暂不可购买");
        }
        if (currentUserId.equals(book.getUserId())) {
            throw new CustomerException("400", "不能购买自己发布的书籍");
        }

        LocalDateTime now = LocalDateTime.now();
        BookOrder order = BookOrder.builder()
            .orderNo(buildOrderNo())
            .bookId(book.getId())
            .buyerId(currentUserId)
            .sellerId(book.getUserId())
            .totalAmount(book.getPrice() == null ? BigDecimal.ZERO : book.getPrice())
            .status(1)
            .buyerMessage(defaultString(buyerMessage, ""))
            .receiverName(defaultString(address.getReceiverName(), ""))
            .receiverPhone(defaultString(address.getReceiverPhone(), ""))
            .receiverAddress(buildFullAddress(address))
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        bookOrderMapper.insert(order);

        book.setStatus(2);
        book.setUpdateTime(now);
        bookMapper.update(book);
        createNotification(book.getUserId(), "order", "你有新的图书订单", "《" + defaultString(book.getTitle(), "书籍") + "》已被下单", "/pages/my/orders?role=seller");
        return toOrderItem(order, currentUserId);
    }

    private UserAddress resolveCheckoutAddress(Long currentUserId, Long addressId) {
        QueryWrapper wrapper = QueryWrapper.create()
            .where("user_id = ?", currentUserId)
            .and("is_deleted = 0");
        if (addressId != null) {
            wrapper.and("id = ?", addressId);
        } else {
            wrapper.and("is_default = 1");
        }
        UserAddress address = userAddressMapper.selectOneByQuery(wrapper.limit(1));
        if (address == null) {
            throw new CustomerException("400", "请先选择收货地址");
        }
        return address;
    }

    private CartItem requireCartItem(Long cartItemId, Long currentUserId) {
        CartItem cartItem = cartItemId == null ? null : cartItemMapper.selectOneById(cartItemId);
        if (cartItem == null || isDeleted(cartItem.getIsDeleted()) || !Objects.equals(cartItem.getUserId(), currentUserId)) {
            throw new CustomerException("404", "购物车记录不存在");
        }
        return cartItem;
    }

    private void removeCheckedCartItems(Long currentUserId, List<Long> cartItemIds) {
        if (cartItemIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long cartItemId : cartItemIds) {
            CartItem cartItem = cartItemMapper.selectOneById(cartItemId);
            if (cartItem != null && Objects.equals(cartItem.getUserId(), currentUserId) && !isDeleted(cartItem.getIsDeleted())) {
                cartItem.setIsDeleted(1);
                cartItem.setUpdateTime(now);
                cartItemMapper.update(cartItem);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return List.of();
    }

    private Book requireBook(Long bookId) {
        if (bookId == null) {
            throw new CustomerException("400", "书籍参数错误");
        }
        Book book = bookMapper.selectOneById(bookId);
        if (book == null || isDeleted(book.getIsDeleted())) {
            throw new CustomerException("404", "书籍不存在");
        }
        return book;
    }

    private BookOrder requireParticipantOrder(Long orderId, Long currentUserId) {
        if (orderId == null) {
            throw new CustomerException("400", "订单参数错误");
        }
        BookOrder order = bookOrderMapper.selectOneById(orderId);
        if (order == null || isDeleted(order.getIsDeleted())) {
            throw new CustomerException("404", "订单不存在");
        }
        if (!currentUserId.equals(order.getBuyerId()) && !currentUserId.equals(order.getSellerId())) {
            throw new CustomerException("403", "无权查看该订单");
        }
        return order;
    }

    private BookOrder requireBuyerOrder(Long orderId, Long currentUserId) {
        BookOrder order = requireParticipantOrder(orderId, currentUserId);
        if (!currentUserId.equals(order.getBuyerId())) {
            throw new CustomerException("403", "仅买家可操作");
        }
        return order;
    }

    private BookOrder requireSellerOrder(Long orderId, Long currentUserId) {
        BookOrder order = requireParticipantOrder(orderId, currentUserId);
        if (!currentUserId.equals(order.getSellerId())) {
            throw new CustomerException("403", "仅卖家可操作");
        }
        return order;
    }

    private Map<String, Object> toOrderItem(BookOrder order, Long currentUserId) {
        Book book = order.getBookId() == null ? null : bookMapper.selectOneById(order.getBookId());
        User seller = order.getSellerId() == null ? null : userMapper.selectOneById(order.getSellerId());
        User buyer = order.getBuyerId() == null ? null : userMapper.selectOneById(order.getBuyerId());
        List<Map<String, Object>> issues = getIssueItems(order.getId(), currentUserId, order);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", order.getId());
        item.put("orderNo", defaultString(order.getOrderNo(), ""));
        item.put("bookId", order.getBookId());
        item.put("bookTitle", book != null ? defaultString(book.getTitle(), "未命名书籍") : "书籍已下架");
        item.put("bookCover", book != null ? resolveBookCover(book) : DEFAULT_AVATAR);
        item.put("totalAmount", order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
        item.put("status", order.getStatus());
        item.put("statusLabel", mapOrderStatus(order.getStatus()));
        item.put("sellerId", order.getSellerId());
        item.put("sellerName", seller != null ? defaultString(seller.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("buyerId", order.getBuyerId());
        item.put("buyerName", buyer != null ? defaultString(buyer.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("createTime", formatTime(order.getCreateTime()));
        item.put("paymentTime", formatTime(order.getPaymentTime()));
        item.put("deliveryTime", formatTime(order.getDeliveryTime()));
        item.put("receiveTime", formatTime(order.getReceiveTime()));
        item.put("receiverName", defaultString(order.getReceiverName(), ""));
        item.put("receiverPhone", defaultString(order.getReceiverPhone(), ""));
        item.put("receiverAddress", defaultString(order.getReceiverAddress(), ""));
        item.put("buyerMessage", defaultString(order.getBuyerMessage(), ""));
        item.put("role", currentUserId.equals(order.getSellerId()) ? "seller" : "buyer");
        item.put("canPay", currentUserId.equals(order.getBuyerId()) && Objects.equals(order.getStatus(), 1));
        item.put("canCancel", currentUserId.equals(order.getBuyerId()) && Objects.equals(order.getStatus(), 1));
        item.put("canConfirm", currentUserId.equals(order.getBuyerId()) && Objects.equals(order.getStatus(), 3));
        item.put("canShip", currentUserId.equals(order.getSellerId()) && Objects.equals(order.getStatus(), 2));
        item.put("canCreateIssue", true);
        item.put("issueCount", issues.size());
        item.put("issues", issues);
        return item;
    }

    private List<Map<String, Object>> getIssueItems(Long orderId, Long currentUserId, BookOrder order) {
        List<OrderIssue> issues = orderIssueMapper.selectListByQuery(
            QueryWrapper.create().where("order_id = ?", orderId).and("is_deleted = 0").orderBy("id desc")
        );
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderIssue issue : issues) {
            items.add(toIssueItem(issue, currentUserId, order));
        }
        return items;
    }

    private Map<String, Object> toIssueItem(OrderIssue issue, Long currentUserId, BookOrder order) {
        User creator = issue.getUserId() == null ? null : userMapper.selectOneById(issue.getUserId());
        User replyUser = issue.getReplyUserId() == null ? null : userMapper.selectOneById(issue.getReplyUserId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", issue.getId());
        item.put("orderId", issue.getOrderId());
        item.put("type", issue.getType());
        item.put("typeLabel", mapIssueTypeLabel(issue.getType()));
        item.put("content", defaultString(issue.getContent(), ""));
        item.put("replyContent", defaultString(issue.getReplyContent(), ""));
        item.put("status", issue.getStatus());
        item.put("statusLabel", mapIssueStatus(issue.getStatus()));
        item.put("creatorId", issue.getUserId());
        item.put("creatorName", creator != null ? defaultString(creator.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("replyUserId", issue.getReplyUserId());
        item.put("replyUserName", replyUser != null ? defaultString(replyUser.getNickname(), DEFAULT_NICKNAME) : "");
        item.put("createTime", formatTime(issue.getCreateTime()));
        item.put("replyTime", formatTime(issue.getReplyTime()));
        item.put("canReply", !currentUserId.equals(issue.getUserId()) && issue.getStatus() != null && issue.getStatus() == 0
            && (currentUserId.equals(order.getBuyerId()) || currentUserId.equals(order.getSellerId())));
        return item;
    }

    private String buildFullAddress(UserAddress address) {
        return defaultString(address.getProvince(), "")
            + defaultString(address.getCity(), "")
            + defaultString(address.getDistrict(), "")
            + " "
            + defaultString(address.getDetailAddress(), "");
    }

    private String buildOrderNo() {
        return "OD" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private void createNotification(Long userId, String type, String title, String content, String routeUrl) {
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Notification notification = Notification.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .content(content)
            .routeUrl(routeUrl)
            .isRead(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        notificationMapper.insert(notification);
    }

    private String mapOrderStatus(Integer status) {
        int value = status == null ? 1 : status;
        return switch (value) {
            case 2 -> "待发货";
            case 3 -> "待收货";
            case 4 -> "已完成";
            case 5 -> "已取消";
            case 6 -> "售后中";
            default -> "待付款";
        };
    }

    private int mapIssueType(String type) {
        return switch (type) {
            case "after_sale" -> 2;
            case "report" -> 3;
            default -> 1;
        };
    }

    private String mapIssueTypeLabel(Integer type) {
        int value = type == null ? 1 : type;
        return switch (value) {
            case 2 -> "售后";
            case 3 -> "举报";
            default -> "疑问";
        };
    }

    private String mapIssueStatus(Integer status) {
        int value = status == null ? 0 : status;
        return switch (value) {
            case 1 -> "已回复";
            case 2 -> "已关闭";
            default -> "待处理";
        };
    }

    private String resolveBookCover(Book book) {
        String images = defaultString(book.getCoverImages(), "");
        if (!images.isBlank()) {
            String[] parts = images.replace("[", "").replace("]", "").replace("\"", "").split(",");
            for (String part : parts) {
                String item = part.trim();
                if (!item.isBlank()) {
                    return item;
                }
            }
        }
        return DEFAULT_AVATAR;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FORMATTER.format(time);
    }

    private boolean isDeleted(Integer isDeleted) {
        return isDeleted != null && isDeleted == 1;
    }

    private String defaultString(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
