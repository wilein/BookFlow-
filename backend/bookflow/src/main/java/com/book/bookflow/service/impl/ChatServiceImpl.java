package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.ChatMessage;
import com.book.bookflow.entity.ChatSession;
import com.book.bookflow.entity.User;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.ChatMessageMapper;
import com.book.bookflow.mapper.ChatSessionMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.service.ChatService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final String DEFAULT_AVATAR = "/static/logo.png";
    private static final String DEFAULT_NICKNAME = "书友";

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BookOrderMapper bookOrderMapper;

    public ChatServiceImpl(
        ChatSessionMapper chatSessionMapper,
        ChatMessageMapper chatMessageMapper,
        UserMapper userMapper,
        BookMapper bookMapper,
        BookOrderMapper bookOrderMapper
    ) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
        this.bookMapper = bookMapper;
        this.bookOrderMapper = bookOrderMapper;
    }

    @Override
    public List<Map<String, Object>> getSessionList() {
        Long currentUserId = AuthContext.requireUserId();
        List<ChatSession> sessions = chatSessionMapper.selectListByQuery(
            QueryWrapper.create()
                .where("(buyer_id = ? or seller_id = ?)", currentUserId, currentUserId)
                .and("is_deleted = 0")
                .orderBy("last_message_time desc, id desc")
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession session : sessions) {
            result.add(toSessionItem(session, currentUserId));
        }
        return result;
    }

    @Override
    public Map<String, Object> openSession(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long sessionId = asLong(payload.get("sessionId"));
        if (sessionId != null) {
            ChatSession existing = chatSessionMapper.selectOneById(sessionId);
            if (existing != null && belongsToUser(existing, currentUserId)) {
                return toSessionItem(existing, currentUserId);
            }
        }

        Long bookId = asLong(payload.get("bookId"));
        Long sellerId = asLong(payload.get("sellerId"));
        if (sellerId == null && bookId != null) {
            Book book = bookMapper.selectOneById(bookId);
            if (book != null) {
                sellerId = book.getUserId();
            }
        }
        if (sellerId == null) {
            sellerId = currentUserId;
        }

        ChatSession session = chatSessionMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("book_id = ?", bookId)
                .and("buyer_id = ?", currentUserId)
                .and("seller_id = ?", sellerId)
                .and("is_deleted = 0")
                .limit(1)
        );

        if (session == null) {
            LocalDateTime now = LocalDateTime.now();
            session = ChatSession.builder()
                .bookId(bookId)
                .buyerId(currentUserId)
                .sellerId(sellerId)
                .lastMessage("")
                .lastMessageTime(now)
                .unreadCount(0)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            chatSessionMapper.insert(session);
        }

        return toSessionItem(session, currentUserId);
    }

    @Override
    public List<Map<String, Object>> getMessageList(Long sessionId) {
        Long currentUserId = AuthContext.requireUserId();
        ChatSession session = sessionId == null ? null : chatSessionMapper.selectOneById(sessionId);
        if (session == null || !belongsToUser(session, currentUserId)) {
            return new ArrayList<>();
        }
        List<ChatMessage> messages = chatMessageMapper.selectListByQuery(
            QueryWrapper.create().where("session_id = ?", sessionId).and("is_deleted = 0").orderBy("id asc")
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage message : messages) {
            result.add(toMessageItem(message, currentUserId));
        }
        markMessagesRead(session, currentUserId);
        return result;
    }

    @Override
    public Map<String, Object> pollMessages(Long sessionId, Long afterId) {
        Long currentUserId = AuthContext.requireUserId();
        ChatSession session = requireSession(sessionId, currentUserId);
        Long cursor = afterId == null ? 0L : afterId;
        List<ChatMessage> messages = chatMessageMapper.selectListByQuery(
            QueryWrapper.create()
                .where("session_id = ?", sessionId)
                .and("id > ?", cursor)
                .and("is_deleted = 0")
                .orderBy("id asc")
        );
        List<Map<String, Object>> items = new ArrayList<>();
        Long latestId = cursor;
        for (ChatMessage message : messages) {
            items.add(toMessageItem(message, currentUserId));
            if (message.getId() != null && message.getId() > latestId) {
                latestId = message.getId();
            }
        }
        markMessagesRead(session, currentUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("latestId", latestId);
        result.put("unreadCount", unreadCount(sessionId, currentUserId));
        return result;
    }

    @Override
    public Map<String, Object> markSessionRead(Long sessionId) {
        Long currentUserId = AuthContext.requireUserId();
        ChatSession session = requireSession(sessionId, currentUserId);
        int count = markMessagesRead(session, currentUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("readCount", count);
        result.put("unreadCount", unreadCount(sessionId, currentUserId));
        return result;
    }

    @Override
    public Map<String, Object> sendMessage(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long sessionId = asLong(payload.get("sessionId"));
        String content = defaultString(payload.get("content"), "");
        LocalDateTime now = LocalDateTime.now();

        ChatSession session = sessionId == null ? null : chatSessionMapper.selectOneById(sessionId);
        if (session == null || !belongsToUser(session, currentUserId)) {
            throw new CustomerException("404", "聊天会话不存在");
        }
        if (content.isBlank()) {
            throw new CustomerException("400", "请输入消息内容");
        }

        ChatMessage message = ChatMessage.builder()
            .sessionId(sessionId)
            .senderId(currentUserId)
            .content(content)
            .isRead(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        chatMessageMapper.insert(message);

        session.setLastMessage(content);
        session.setLastMessageTime(now);
        session.setUpdateTime(now);
        chatSessionMapper.update(session);
        return toMessageItem(message, currentUserId);
    }

    private Map<String, Object> toSessionItem(ChatSession session, Long currentUserId) {
        Long peerUserId = currentUserId.equals(session.getBuyerId()) ? session.getSellerId() : session.getBuyerId();
        User peerUser = peerUserId == null ? null : userMapper.selectOneById(peerUserId);
        User currentUser = userMapper.selectOneById(currentUserId);
        User seller = session.getSellerId() == null ? null : userMapper.selectOneById(session.getSellerId());
        User buyer = session.getBuyerId() == null ? null : userMapper.selectOneById(session.getBuyerId());
        Book book = session.getBookId() == null ? null : bookMapper.selectOneById(session.getBookId());
        BookOrder order = findLatestOrder(session);
        long currentUnreadCount = unreadCount(session.getId(), currentUserId);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", session.getId());
        item.put("sessionId", session.getId());
        item.put("sellerId", session.getSellerId());
        item.put("buyerId", session.getBuyerId());
        item.put("bookId", session.getBookId());
        item.put("peerUserId", peerUserId);
        item.put("role", currentUserId.equals(session.getSellerId()) ? "seller" : "buyer");
        item.put("name", peerUser != null ? defaultString(peerUser.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("avatar", peerUser != null ? defaultString(peerUser.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR);
        item.put("selfAvatar", currentUser != null ? defaultString(currentUser.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR);
        item.put("selfName", currentUser != null ? defaultString(currentUser.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("sellerName", seller != null ? defaultString(seller.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("sellerAvatar", seller != null ? defaultString(seller.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR);
        item.put("buyerName", buyer != null ? defaultString(buyer.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("buyerAvatar", buyer != null ? defaultString(buyer.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR);
        item.put("bookTitle", book != null ? defaultString(book.getTitle(), "书籍") : "书籍");
        item.put("bookCover", book != null ? resolveBookCover(book) : DEFAULT_AVATAR);
        item.put("bookPrice", book != null && book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO);
        item.put("bookStatus", book != null ? book.getStatus() : 1);
        item.put("bookStatusLabel", mapBookStatus(book != null ? book.getStatus() : 1));
        item.put("preview", defaultString(session.getLastMessage(), "发起聊天吧"));
        item.put("time", session.getLastMessageTime() == null ? "" : TIME_FORMATTER.format(session.getLastMessageTime()));
        item.put("unread", currentUnreadCount);
        item.put("unreadCount", currentUnreadCount);
        item.put("orderId", order != null ? order.getId() : null);
        item.put("orderNo", order != null ? defaultString(order.getOrderNo(), "") : "");
        item.put("orderStatus", order != null ? order.getStatus() : null);
        item.put("orderStatusLabel", order != null ? mapOrderStatus(order.getStatus()) : "未下单");
        item.put("canPay", order != null && currentUserId.equals(order.getBuyerId()) && order.getStatus() != null && order.getStatus() == 1);
        item.put("canCancel", order != null && currentUserId.equals(order.getBuyerId()) && order.getStatus() != null && order.getStatus() == 1);
        item.put("canConfirm", order != null && currentUserId.equals(order.getBuyerId()) && order.getStatus() != null && order.getStatus() == 3);
        item.put("canShip", order != null && currentUserId.equals(order.getSellerId()) && order.getStatus() != null && order.getStatus() == 2);
        item.put("canCreateOrder", order == null && currentUserId.equals(session.getBuyerId()) && book != null
            && book.getStatus() != null && book.getStatus() == 1);
        return item;
    }

    private Map<String, Object> toMessageItem(ChatMessage message, Long currentUserId) {
        User sender = message.getSenderId() == null ? null : userMapper.selectOneById(message.getSenderId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", message.getId());
        item.put("sessionId", message.getSessionId());
        item.put("senderId", message.getSenderId());
        item.put("senderName", sender != null ? defaultString(sender.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("senderAvatar", sender != null ? defaultString(sender.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR);
        item.put("content", defaultString(message.getContent(), ""));
        item.put("mine", message.getSenderId() != null && message.getSenderId().equals(currentUserId));
        item.put("time", message.getCreateTime() == null ? "" : TIME_FORMATTER.format(message.getCreateTime()));
        return item;
    }

    private BookOrder findLatestOrder(ChatSession session) {
        if (session.getBookId() == null || session.getBuyerId() == null || session.getSellerId() == null) {
            return null;
        }
        return bookOrderMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("book_id = ?", session.getBookId())
                .and("buyer_id = ?", session.getBuyerId())
                .and("seller_id = ?", session.getSellerId())
                .and("is_deleted = 0")
                .orderBy("id desc")
                .limit(1)
        );
    }

    private ChatSession requireSession(Long sessionId, Long currentUserId) {
        ChatSession session = sessionId == null ? null : chatSessionMapper.selectOneById(sessionId);
        if (session == null || !belongsToUser(session, currentUserId)) {
            throw new CustomerException("404", "聊天会话不存在");
        }
        return session;
    }

    private int markMessagesRead(ChatSession session, Long currentUserId) {
        List<ChatMessage> unreadMessages = chatMessageMapper.selectListByQuery(
            QueryWrapper.create()
                .where("session_id = ?", session.getId())
                .and("sender_id <> ?", currentUserId)
                .and("is_read = 0")
                .and("is_deleted = 0")
        );
        LocalDateTime now = LocalDateTime.now();
        for (ChatMessage message : unreadMessages) {
            message.setIsRead(1);
            message.setUpdateTime(now);
            chatMessageMapper.update(message);
        }
        session.setUnreadCount(0);
        session.setUpdateTime(now);
        chatSessionMapper.update(session);
        return unreadMessages.size();
    }

    private long unreadCount(Long sessionId, Long currentUserId) {
        if (sessionId == null || currentUserId == null) {
            return 0;
        }
        return chatMessageMapper.selectCountByQuery(
            QueryWrapper.create()
                .where("session_id = ?", sessionId)
                .and("sender_id <> ?", currentUserId)
                .and("is_read = 0")
                .and("is_deleted = 0")
        );
    }

    private boolean belongsToUser(ChatSession session, Long currentUserId) {
        return currentUserId.equals(session.getBuyerId()) || currentUserId.equals(session.getSellerId());
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

    private String mapBookStatus(Integer status) {
        int value = status == null ? 1 : status;
        return switch (value) {
            case 2 -> "交易中";
            case 3 -> "已售";
            case 4 -> "已下架";
            default -> "在售";
        };
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

    private String defaultString(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
