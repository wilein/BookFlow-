package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Annotation;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.LearningPath;
import com.book.bookflow.entity.Notification;
import com.book.bookflow.entity.Resource;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserAddress;
import com.book.bookflow.entity.UserBrowseHistory;
import com.book.bookflow.entity.UserFeedback;
import com.book.bookflow.entity.UserFavorite;
import com.book.bookflow.entity.UserProfile;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.AnnotationMapper;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.LearningPathMapper;
import com.book.bookflow.mapper.NotificationMapper;
import com.book.bookflow.mapper.PathNodeMapper;
import com.book.bookflow.mapper.ResourceMapper;
import com.book.bookflow.mapper.UserAddressMapper;
import com.book.bookflow.mapper.UserBrowseHistoryMapper;
import com.book.bookflow.mapper.UserFeedbackMapper;
import com.book.bookflow.mapper.UserFavoriteMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.mapper.UserProfileMapper;
import com.book.bookflow.service.ProfileService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final String DEFAULT_NICKNAME = "书友";
    private static final String DEFAULT_SIGNATURE = "个人信息待完善";
    private static final String DEFAULT_AVATAR = "/static/logo.png";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Value("${app.image-base-url:}")
    private String imageBaseUrl;

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final BookMapper bookMapper;
    private final AnnotationMapper annotationMapper;
    private final LearningPathMapper learningPathMapper;
    private final PathNodeMapper pathNodeMapper;
    private final ResourceMapper resourceMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final BookOrderMapper bookOrderMapper;
    private final UserAddressMapper userAddressMapper;
    private final UserBrowseHistoryMapper userBrowseHistoryMapper;
    private final UserFeedbackMapper userFeedbackMapper;
    private final NotificationMapper notificationMapper;

    public ProfileServiceImpl(UserMapper userMapper,
                              UserProfileMapper userProfileMapper,
                              BookMapper bookMapper,
                              AnnotationMapper annotationMapper,
                              LearningPathMapper learningPathMapper,
                              PathNodeMapper pathNodeMapper,
                              ResourceMapper resourceMapper,
                              UserFavoriteMapper userFavoriteMapper,
                              BookOrderMapper bookOrderMapper,
                              UserAddressMapper userAddressMapper,
                              UserBrowseHistoryMapper userBrowseHistoryMapper,
                              UserFeedbackMapper userFeedbackMapper,
                              NotificationMapper notificationMapper) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.bookMapper = bookMapper;
        this.annotationMapper = annotationMapper;
        this.learningPathMapper = learningPathMapper;
        this.pathNodeMapper = pathNodeMapper;
        this.resourceMapper = resourceMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.bookOrderMapper = bookOrderMapper;
        this.userAddressMapper = userAddressMapper;
        this.userBrowseHistoryMapper = userBrowseHistoryMapper;
        this.userFeedbackMapper = userFeedbackMapper;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public Map<String, Object> getProfile() {
        Long currentUserId = AuthContext.requireUserId();
        User user = getUserOrThrow(currentUserId);
        UserProfile profile = getOrCreateProfile(currentUserId, true);
        return buildProfile(user, profile);
    }

    @Override
    public Map<String, Object> getUserStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        Long currentUserId = AuthContext.requireUserId();
        result.put("sellingBooks", countBookshelf(currentUserId, "selling"));
        result.put("soldBooks", countBookshelf(currentUserId, "sold"));
        result.put("favorites", userFavoriteMapper.selectCountByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId)
        ));
        result.put("annotations", annotationMapper.selectCountByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("is_deleted = 0")
        ));
        result.put("paths", learningPathMapper.selectCountByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("is_deleted = 0")
        ));
        result.put("resources", resourceMapper.selectCountByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("is_deleted = 0")
        ));
        result.put("pendingPay", countOrders(currentUserId, 1, "buyer"));
        result.put("pendingShip", countOrders(currentUserId, 2, "seller"));
        result.put("pendingReceive", countOrders(currentUserId, 3, "buyer"));
        return result;
    }

    @Override
    public Map<String, Object> updateProfile(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        User user = getUserOrThrow(currentUserId);
        user.setNickname(defaultString(payload.get("nickname"), DEFAULT_NICKNAME));
        user.setAvatarUrl(defaultString(payload.get("avatarUrl"), user.getAvatarUrl()));
        user.setMobile(defaultString(payload.get("mobile"), ""));
        user.setCity(defaultString(payload.get("city"), ""));
        user.setProvince(defaultString(payload.get("province"), ""));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);

        UserProfile profile = getOrCreateProfile(currentUserId, true);
        profile.setRealName(defaultString(payload.get("realName"), profile.getRealName()));
        profile.setStudentId(defaultString(payload.get("studentId"), profile.getStudentId()));
        profile.setSchool(defaultString(payload.get("school"), ""));
        profile.setDepartment(defaultString(payload.get("department"), ""));
        profile.setIntro(defaultString(payload.get("intro"), DEFAULT_SIGNATURE));
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.update(profile);
        return buildProfile(user, profile);
    }

    @Override
    public Map<String, Object> submitStudentVerify(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        User user = getUserOrThrow(currentUserId);
        UserProfile profile = getOrCreateProfile(currentUserId, true);

        String realName = defaultString(payload.get("realName"), "");
        String studentId = defaultString(payload.get("studentId"), "");
        String school = defaultString(payload.get("school"), "");
        String department = defaultString(payload.get("department"), "");
        String studentCardImageUrl = defaultString(payload.get("studentCardImageUrl"), "");
        String verifyType = defaultString(payload.get("verifyType"), "student_card");

        if (realName.isBlank()) {
            throw new CustomerException("400", "请输入真实姓名");
        }
        if (studentId.isBlank()) {
            throw new CustomerException("400", "请输入学号");
        }
        if (studentCardImageUrl.isBlank()) {
            throw new CustomerException("400", "请上传学生证照片");
        }
        resolveOwnStudentCardImagePath(studentCardImageUrl);

        profile.setRealName(realName);
        profile.setStudentId(studentId);
        profile.setSchool(school);
        profile.setDepartment(department);
        profile.setStudentCardImageUrl(studentCardImageUrl);
        profile.setVerifyType(verifyType);
        profile.setVerifySubmitTime(LocalDateTime.now());
        profile.setAuditRemark("");
        profile.setAuthStatus(1);
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.update(profile);

        if (defaultString(user.getNickname(), "").isBlank()) {
            user.setNickname(DEFAULT_NICKNAME);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.update(user);
        }

        return buildProfile(user, profile);
    }

    @Override
    public Map<String, Object> uploadProfileImage(MultipartFile file, String type) {
        Long currentUserId = AuthContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new CustomerException("400", "请选择图片");
        }

        String normalizedType = normalizeUploadType(type);
        String extension = resolveExtension(file.getOriginalFilename());
        String subDirectory = "avatar".equals(normalizedType) ? "avatar" : "student-card";
        String fileName = currentUserId + "_" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().replace("-", "") + extension;

        Path targetDirectory = Paths.get("uploads", "profile", subDirectory);
        Path targetFile = targetDirectory.resolve(fileName);
        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CustomerException("500", "图片上传失败");
        }

        String relativePath = "profile/" + subDirectory + "/" + fileName;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", normalizeBaseUrl(imageBaseUrl) + "uploads/" + relativePath);
        result.put("fileName", fileName);
        result.put("type", normalizedType);
        return result;
    }

    @Override
    public Path resolveOwnStudentCardImagePath(String rawUrl) {
        Long currentUserId = AuthContext.requireUserId();
        String fileName = extractStudentCardFileName(rawUrl);
        if (fileName.isBlank()) {
            throw new CustomerException("400", "学生证图片地址错误");
        }
        if (!fileName.startsWith(currentUserId + "_")) {
            throw new CustomerException("403", "无权查看该学生证图片");
        }
        Path directory = Paths.get("uploads", "profile", "student-card").toAbsolutePath().normalize();
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory) || !Files.isRegularFile(target)) {
            throw new CustomerException("404", "学生证图片不存在");
        }
        return target;
    }

    private String extractStudentCardFileName(String rawUrl) {
        String text = defaultString(rawUrl, "");
        if (text.isBlank()) {
            return "";
        }
        int queryIndex = text.indexOf('?');
        if (queryIndex >= 0) {
            text = text.substring(0, queryIndex);
        }
        int hashIndex = text.indexOf('#');
        if (hashIndex >= 0) {
            text = text.substring(0, hashIndex);
        }
        text = text.replace('\\', '/');
        int markerIndex = text.lastIndexOf("/student-card/");
        if (markerIndex >= 0) {
            text = text.substring(markerIndex + "/student-card/".length());
        } else {
            int slashIndex = text.lastIndexOf('/');
            if (slashIndex >= 0) {
                text = text.substring(slashIndex + 1);
            }
        }
        if (text.contains("/") || text.contains("..")) {
            return "";
        }
        return text.trim();
    }

    @Override
    public List<Map<String, Object>> getMyBookshelf(String status) {
        Long currentUserId = AuthContext.requireUserId();
        User currentUser = getUserOrThrow(currentUserId);
        UserProfile profile = getOrCreateProfile(currentUserId, true);

        QueryWrapper wrapper = QueryWrapper.create()
            .where("user_id = ?", currentUserId)
            .and("is_deleted = 0")
            .orderBy("id desc")
            .limit(100);
        if ("sold".equalsIgnoreCase(status)) {
            wrapper.and("status = 3");
        } else {
            wrapper.and("status in (1, 2)");
        }

        List<Book> books = bookMapper.selectListByQuery(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Book book : books) {
            result.add(toBookItem(book, currentUser, profile));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyOrders(String status, String role) {
        Long currentUserId = AuthContext.requireUserId();
        String normalizedRole = "seller".equalsIgnoreCase(defaultString(role, "")) ? "seller" : "buyer";
        QueryWrapper wrapper = QueryWrapper.create()
            .where(("seller".equals(normalizedRole) ? "seller_id" : "buyer_id") + " = ?", currentUserId)
            .and("is_deleted = 0")
            .orderBy("id desc")
            .limit(100);

        Integer statusValue = parseInteger(status);
        if (statusValue != null) {
            wrapper.and("status = ?", statusValue);
        }

        List<BookOrder> orders = bookOrderMapper.selectListByQuery(wrapper);
        Map<Long, Book> booksById = loadBooksByIds(
            orders.stream().map(BookOrder::getBookId).collect(Collectors.toList())
        );
        Map<Long, User> usersById = loadUsersByIds(
            orders.stream()
                .flatMap(order -> Stream.of(order.getSellerId(), order.getBuyerId()))
                .collect(Collectors.toList())
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (BookOrder order : orders) {
            Book book = order.getBookId() == null ? null : booksById.get(order.getBookId());
            User seller = order.getSellerId() == null ? null : usersById.get(order.getSellerId());
            User buyer = order.getBuyerId() == null ? null : usersById.get(order.getBuyerId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", order.getId());
            item.put("orderNo", defaultString(order.getOrderNo(), ""));
            item.put("bookId", order.getBookId());
            item.put("bookTitle", book != null ? defaultString(book.getTitle(), "未命名书籍") : "书籍已下架");
            item.put("bookCover", book != null ? resolveBookCover(book) : DEFAULT_AVATAR);
            item.put("totalAmount", order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
            item.put("status", order.getStatus());
            item.put("statusLabel", mapOrderStatus(order.getStatus()));
            item.put("role", normalizedRole);
            item.put("buyerId", order.getBuyerId());
            item.put("buyerName", buyer != null ? defaultString(buyer.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
            item.put("sellerId", order.getSellerId());
            item.put("sellerName", seller != null ? defaultString(seller.getNickname(), DEFAULT_NICKNAME) : "卖家");
            item.put("createTime", formatTime(order.getCreateTime()));
            item.put("paymentTime", formatTime(order.getPaymentTime()));
            item.put("deliveryTime", formatTime(order.getDeliveryTime()));
            item.put("receiveTime", formatTime(order.getReceiveTime()));
            item.put("receiverName", defaultString(order.getReceiverName(), ""));
            item.put("receiverPhone", defaultString(order.getReceiverPhone(), ""));
            item.put("receiverAddress", defaultString(order.getReceiverAddress(), ""));
            item.put("buyerMessage", defaultString(order.getBuyerMessage(), ""));
            item.put("canPay", "buyer".equals(normalizedRole) && Objects.equals(order.getStatus(), 1));
            item.put("canCancel", "buyer".equals(normalizedRole) && Objects.equals(order.getStatus(), 1));
            item.put("canConfirm", "buyer".equals(normalizedRole) && Objects.equals(order.getStatus(), 3));
            item.put("canShip", "seller".equals(normalizedRole) && Objects.equals(order.getStatus(), 2));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyFavorites(String type) {
        Long currentUserId = AuthContext.requireUserId();
        int targetType = "path".equalsIgnoreCase(type) ? 2 : 1;
        List<UserFavorite> favorites = userFavoriteMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("target_type = ?", targetType)
                .orderBy("id desc")
                .limit(100)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserFavorite favorite : favorites) {
            if (targetType == 1) {
                Book book = favorite.getTargetId() == null ? null : bookMapper.selectOneById(favorite.getTargetId());
                if (book == null || isDeleted(book.getIsDeleted())) {
                    continue;
                }
                User seller = book.getUserId() == null ? null : userMapper.selectOneById(book.getUserId());
                UserProfile sellerProfile = book.getUserId() == null ? null : getOrCreateProfile(book.getUserId(), true);
                result.add(toBookItem(book, seller, sellerProfile));
            } else {
                LearningPath path = favorite.getTargetId() == null ? null : learningPathMapper.selectOneById(favorite.getTargetId());
                if (path == null || isDeleted(path.getIsDeleted())) {
                    continue;
                }
                result.add(toPathItem(path));
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyAnnotations() {
        Long currentUserId = AuthContext.requireUserId();
        List<Annotation> annotations = annotationMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("is_deleted = 0")
                .orderBy("id desc")
                .limit(300)
        );

        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Annotation annotation : annotations) {
            Book book = annotation.getBookId() == null ? null : bookMapper.selectOneById(annotation.getBookId());
            String groupKey = annotation.getBookId() == null ? "unknown-" + annotation.getId() : String.valueOf(annotation.getBookId());
            Map<String, Object> group = grouped.get(groupKey);
            if (group == null) {
                group = new LinkedHashMap<>();
                group.put("id", "book-" + groupKey);
                group.put("bookId", annotation.getBookId());
                group.put("bookTitle", book != null ? defaultString(book.getTitle(), "书籍已下架") : "书籍已下架");
                group.put("bookCover", book != null ? resolveBookCover(book) : DEFAULT_AVATAR);
                group.put("cover", book != null ? resolveBookCover(book) : DEFAULT_AVATAR);
                group.put("category", book != null ? defaultString(book.getCategory(), "未分类") : "已下架");
                group.put("annotationCount", 0);
                group.put("pageCount", 0);
                group.put("pageNums", new ArrayList<Integer>());
                group.put("latestAnnotationId", annotation.getId());
                group.put("latestContent", defaultString(annotation.getContent(), ""));
                group.put("latestType", annotation.getType());
                group.put("latestTypeLabel", mapAnnotationType(annotation.getType()));
                group.put("latestPageNum", annotation.getPageNum());
                group.put("latestTime", formatTime(annotation.getCreateTime()));
                grouped.put(groupKey, group);
            }

            group.put("annotationCount", ((Integer) group.get("annotationCount")) + 1);
            Integer pageNum = annotation.getPageNum();
            @SuppressWarnings("unchecked")
            List<Integer> pageNums = (List<Integer>) group.get("pageNums");
            if (pageNum != null && !pageNums.contains(pageNum)) {
                pageNums.add(pageNum);
                group.put("pageCount", pageNums.size());
            }
        }
        return new ArrayList<>(grouped.values());
    }

    @Override
    public List<Map<String, Object>> getMyPaths() {
        Long currentUserId = AuthContext.requireUserId();
        List<LearningPath> paths = learningPathMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("is_deleted = 0")
                .orderBy("id desc")
                .limit(100)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (LearningPath path : paths) {
            result.add(toPathItem(path));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getAddressList() {
        Long currentUserId = AuthContext.requireUserId();
        List<UserAddress> addresses = userAddressMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("is_deleted = 0")
                .orderBy("is_default desc, id desc")
                .limit(100)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserAddress address : addresses) {
            result.add(toAddressItem(address));
        }
        return result;
    }

    @Override
    public Map<String, Object> saveAddress(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long id = asLong(payload.get("id"));
        LocalDateTime now = LocalDateTime.now();

        String receiverName = defaultString(payload.get("receiverName"), "");
        String receiverPhone = defaultString(payload.get("receiverPhone"), "");
        String province = defaultString(payload.get("province"), "");
        String city = defaultString(payload.get("city"), "");
        String district = defaultString(payload.get("district"), "");
        String detailAddress = defaultString(payload.get("detailAddress"), "");
        boolean requestedDefault = parseBoolean(payload.get("isDefault"));

        if (receiverName.isBlank()) {
            throw new CustomerException("400", "请输入收货人姓名");
        }
        if (receiverPhone.isBlank()) {
            throw new CustomerException("400", "请输入收货人电话");
        }
        if (province.isBlank() || city.isBlank() || district.isBlank()) {
            throw new CustomerException("400", "请完整填写省市区");
        }
        if (detailAddress.isBlank()) {
            throw new CustomerException("400", "请输入详细地址");
        }

        UserAddress address;
        boolean isNew = id == null;
        if (isNew) {
            address = new UserAddress();
            address.setUserId(currentUserId);
            address.setCreateTime(now);
            address.setIsDeleted(0);
            address.setIsDefault(0);
        } else {
            address = userAddressMapper.selectOneById(id);
            if (address == null || !currentUserId.equals(address.getUserId()) || isDeleted(address.getIsDeleted())) {
                throw new CustomerException("404", "地址不存在");
            }
        }

        address.setReceiverName(receiverName);
        address.setReceiverPhone(receiverPhone);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setDetailAddress(detailAddress);
        address.setUpdateTime(now);

        if (isNew) {
            userAddressMapper.insert(address);
        } else {
            userAddressMapper.update(address);
        }

        boolean shouldDefault = requestedDefault || countActiveAddresses(currentUserId) == 1;
        if (shouldDefault) {
            clearDefaultAddress(currentUserId, address.getId());
            address.setIsDefault(1);
            address.setUpdateTime(LocalDateTime.now());
            userAddressMapper.update(address);
        } else if (!hasDefaultAddress(currentUserId)) {
            address.setIsDefault(1);
            address.setUpdateTime(LocalDateTime.now());
            userAddressMapper.update(address);
        }
        return toAddressItem(address);
    }

    @Override
    public Map<String, Object> deleteAddress(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long id = asLong(payload.get("id"));
        if (id == null) {
            throw new CustomerException("400", "地址参数错误");
        }

        UserAddress address = userAddressMapper.selectOneById(id);
        if (address == null || !currentUserId.equals(address.getUserId()) || isDeleted(address.getIsDeleted())) {
            throw new CustomerException("404", "地址不存在");
        }

        boolean wasDefault = address.getIsDefault() != null && address.getIsDefault() == 1;
        address.setIsDeleted(1);
        address.setIsDefault(0);
        address.setUpdateTime(LocalDateTime.now());
        userAddressMapper.update(address);

        if (wasDefault) {
            List<UserAddress> remain = userAddressMapper.selectListByQuery(
                QueryWrapper.create()
                    .where("user_id = ?", currentUserId)
                    .and("is_deleted = 0")
                    .orderBy("id desc")
                    .limit(1)
            );
            if (!remain.isEmpty()) {
                UserAddress next = remain.get(0);
                next.setIsDefault(1);
                next.setUpdateTime(LocalDateTime.now());
                userAddressMapper.update(next);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("deleted", true);
        return result;
    }

    @Override
    public Map<String, Object> setDefaultAddress(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long id = asLong(payload.get("id"));
        if (id == null) {
            throw new CustomerException("400", "地址参数错误");
        }

        UserAddress address = userAddressMapper.selectOneById(id);
        if (address == null || !currentUserId.equals(address.getUserId()) || isDeleted(address.getIsDeleted())) {
            throw new CustomerException("404", "地址不存在");
        }

        clearDefaultAddress(currentUserId, address.getId());
        address.setIsDefault(1);
        address.setUpdateTime(LocalDateTime.now());
        userAddressMapper.update(address);
        return toAddressItem(address);
    }

    @Override
    public List<Map<String, Object>> getBrowseHistory() {
        Long currentUserId = AuthContext.requireUserId();
        List<UserBrowseHistory> histories = userBrowseHistoryMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("is_deleted = 0")
                .orderBy("last_view_time desc, id desc")
                .limit(100)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBrowseHistory history : histories) {
            result.add(toBrowseHistoryItem(history));
        }
        return result;
    }

    @Override
    public Map<String, Object> recordBrowseHistory(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        String targetType = defaultString(payload.get("targetType"), "").toLowerCase();
        Long targetId = asLong(payload.get("targetId"));
        if (!"book".equals(targetType) && !"path".equals(targetType)) {
            throw new CustomerException("400", "\u6d4f\u89c8\u7c7b\u578b\u9519\u8bef");
        }
        if (targetId == null) {
            Map<String, Object> ignored = new LinkedHashMap<>();
            ignored.put("ignored", true);
            ignored.put("targetType", targetType);
            return ignored;
        }

        LocalDateTime now = LocalDateTime.now();
        UserBrowseHistory history = userBrowseHistoryMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("target_type = ?", targetType)
                .and("target_id = ?", targetId)
                .limit(1)
        );

        if (history == null) {
            history = new UserBrowseHistory();
            history.setUserId(currentUserId);
            history.setTargetType(targetType);
            history.setTargetId(targetId);
            history.setCreateTime(now);
            history.setIsDeleted(0);
        }

        history.setTitle(defaultString(payload.get("title"), targetType));
        history.setSubTitle(defaultString(payload.get("subTitle"), ""));
        history.setCoverUrl(defaultString(payload.get("coverUrl"), DEFAULT_AVATAR));
        history.setRouteUrl(normalizeRouteUrl(payload.get("routeUrl")));
        history.setLastViewTime(now);
        history.setUpdateTime(now);
        history.setIsDeleted(0);

        if (history.getId() == null) {
            try {
                userBrowseHistoryMapper.insert(history);
            } catch (DuplicateKeyException exception) {
                UserBrowseHistory existing = userBrowseHistoryMapper.selectOneByQuery(
                    QueryWrapper.create()
                        .where("user_id = ?", currentUserId)
                        .and("target_type = ?", targetType)
                        .and("target_id = ?", targetId)
                        .limit(1)
                );
                if (existing == null) {
                    throw exception;
                }
                existing.setTitle(history.getTitle());
                existing.setSubTitle(history.getSubTitle());
                existing.setCoverUrl(history.getCoverUrl());
                existing.setRouteUrl(history.getRouteUrl());
                existing.setLastViewTime(now);
                existing.setUpdateTime(now);
                existing.setIsDeleted(0);
                userBrowseHistoryMapper.update(existing);
                history = existing;
            }
        } else {
            userBrowseHistoryMapper.update(history);
        }
        return toBrowseHistoryItem(history);
    }

    @Override
    public Map<String, Object> deleteBrowseHistory(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long id = asLong(payload.get("id"));
        if (id == null) {
            throw new CustomerException("400", "\u5386\u53f2\u8bb0\u5f55\u53c2\u6570\u9519\u8bef");
        }
        UserBrowseHistory history = userBrowseHistoryMapper.selectOneById(id);
        if (history == null || !currentUserId.equals(history.getUserId()) || isDeleted(history.getIsDeleted())) {
            throw new CustomerException("404", "\u5386\u53f2\u8bb0\u5f55\u4e0d\u5b58\u5728");
        }
        history.setIsDeleted(1);
        history.setUpdateTime(LocalDateTime.now());
        userBrowseHistoryMapper.update(history);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("deleted", true);
        return result;
    }

    @Override
    public Map<String, Object> clearBrowseHistory() {
        Long currentUserId = AuthContext.requireUserId();
        List<UserBrowseHistory> histories = userBrowseHistoryMapper.selectListByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("is_deleted = 0")
        );
        LocalDateTime now = LocalDateTime.now();
        for (UserBrowseHistory history : histories) {
            history.setIsDeleted(1);
            history.setUpdateTime(now);
            userBrowseHistoryMapper.update(history);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cleared", true);
        result.put("count", histories.size());
        return result;
    }

    @Override
    public Map<String, Object> submitFeedback(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        String feedbackType = defaultString(payload.get("feedbackType"), "");
        String content = defaultString(payload.get("content"), "");
        String contact = defaultString(payload.get("contact"), "");
        String pagePath = defaultString(payload.get("pagePath"), "");

        if (feedbackType.isBlank()) {
            throw new CustomerException("400", "\u8bf7\u9009\u62e9\u53cd\u9988\u7c7b\u578b");
        }
        if (content.isBlank()) {
            throw new CustomerException("400", "\u8bf7\u586b\u5199\u53cd\u9988\u5185\u5bb9");
        }

        LocalDateTime now = LocalDateTime.now();
        UserFeedback feedback = UserFeedback.builder()
            .userId(currentUserId)
            .feedbackType(feedbackType)
            .content(content)
            .contact(contact)
            .pagePath(pagePath)
            .status(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        userFeedbackMapper.insert(feedback);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", feedback.getId());
        result.put("feedbackType", feedbackType);
        result.put("status", 0);
        return result;
    }

    @Override
    public List<Map<String, Object>> getNotifications() {
        Long currentUserId = AuthContext.requireUserId();
        List<Notification> notifications = notificationMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("is_deleted = 0")
                .orderBy("id desc")
                .limit(100)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification notification : notifications) {
            result.add(toNotificationItem(notification));
        }
        return result;
    }

    @Override
    public Map<String, Object> readNotification(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long notificationId = asLong(payload.get("id"));
        if (notificationId == null) {
            throw new CustomerException("400", "通知参数错误");
        }
        Notification notification = notificationMapper.selectOneById(notificationId);
        if (notification == null || !currentUserId.equals(notification.getUserId()) || isDeleted(notification.getIsDeleted())) {
            throw new CustomerException("404", "通知不存在");
        }
        notification.setIsRead(1);
        notification.setUpdateTime(LocalDateTime.now());
        notificationMapper.update(notification);
        return toNotificationItem(notification);
    }

    private Map<String, Object> buildProfile(User user, UserProfile profile) {
        Map<String, Object> result = new LinkedHashMap<>();
        String nickname = defaultString(user.getNickname(), DEFAULT_NICKNAME);
        String avatarUrl = defaultString(user.getAvatarUrl(), DEFAULT_AVATAR);
        String school = defaultString(profile.getSchool(), "");
        String department = defaultString(profile.getDepartment(), "");
        String intro = defaultString(profile.getIntro(), DEFAULT_SIGNATURE);
        int creditScore = profile.getCreditScore() == null ? 88 : profile.getCreditScore();
        int authStatus = normalizeAuthStatus(profile);

        result.put("id", user.getId());
        result.put("userId", user.getId());
        result.put("nickname", nickname);
        result.put("displayName", nickname);
        result.put("avatar", avatarUrl);
        result.put("avatarUrl", avatarUrl);
        result.put("mobile", defaultString(user.getMobile(), ""));
        result.put("city", defaultString(user.getCity(), ""));
        result.put("province", defaultString(user.getProvince(), ""));
        result.put("school", school);
        result.put("department", department);
        result.put("intro", intro);
        result.put("realName", defaultString(profile.getRealName(), ""));
        result.put("studentId", defaultString(profile.getStudentId(), ""));
        result.put("studentCardImageUrl", defaultString(profile.getStudentCardImageUrl(), ""));
        result.put("verifyType", defaultString(profile.getVerifyType(), ""));
        result.put("verifySubmitTime", formatTime(profile.getVerifySubmitTime()));
        result.put("auditRemark", defaultString(profile.getAuditRemark(), ""));
        result.put("creditScore", creditScore);
        result.put("points", creditScore);
        result.put("level", "Lv." + Math.max(1, Math.min(9, (creditScore + 9) / 10)));
        result.put("creditBadge", mapCreditBadge(creditScore));
        result.put("authStatus", authStatus);
        result.put("authLabel", mapAuthLabel(authStatus));
        result.put("verified", authStatus == 2);
        result.put("signature", buildSignature(school, department, intro));
        result.put("profileIncomplete", isProfileIncomplete(nickname, school, department, intro));
        return result;
    }

    private UserProfile getOrCreateProfile(Long userId, boolean createWhenMissing) {
        UserProfile profile = userProfileMapper.selectOneByQuery(
            QueryWrapper.create().where("user_id = ?", userId).and("is_deleted = 0").limit(1)
        );
        if (profile != null || !createWhenMissing) {
            return profile;
        }

        LocalDateTime now = LocalDateTime.now();
        UserProfile created = UserProfile.builder()
            .userId(userId)
            .studentId("")
            .realName("")
            .school("")
            .department("")
            .authStatus(0)
            .creditScore(88)
            .intro(DEFAULT_SIGNATURE)
            .studentCardImageUrl("")
            .verifyType("")
            .auditRemark("")
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        userProfileMapper.insert(created);
        return created;
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null || isDeleted(user.getIsDeleted())) {
            throw new CustomerException("404", "用户不存在");
        }
        return user;
    }

    private long countBookshelf(Long userId, String status) {
        QueryWrapper wrapper = QueryWrapper.create().where("user_id = ?", userId).and("is_deleted = 0");
        if ("sold".equalsIgnoreCase(status)) {
            wrapper.and("status = 3");
        } else {
            wrapper.and("status in (1, 2)");
        }
        return bookMapper.selectCountByQuery(wrapper);
    }

    private long countOrders(Long userId, int status, String role) {
        String column = "seller".equalsIgnoreCase(defaultString(role, "")) ? "seller_id" : "buyer_id";
        return bookOrderMapper.selectCountByQuery(
            QueryWrapper.create()
                .where(column + " = ?", userId)
                .and("status = ?", status)
                .and("is_deleted = 0")
        );
    }

    private String normalizeUploadType(String type) {
        String normalized = defaultString(type, "avatar").toLowerCase();
        return normalized.contains("student") ? "studentCard" : "avatar";
    }

    private String resolveExtension(String originalFilename) {
        String fileName = defaultString(originalFilename, "");
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return ".jpg";
        }
        String extension = fileName.substring(index).toLowerCase();
        if (extension.length() > 8 || extension.contains("/") || extension.contains("\\")) {
            return ".jpg";
        }
        return extension;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String text = defaultString(baseUrl, "/");
        return text.endsWith("/") ? text : text + "/";
    }

    private String normalizeRouteUrl(Object value) {
        String routeUrl = defaultString(value, "");
        if (routeUrl.length() <= 2048) {
            return routeUrl;
        }
        return routeUrl.substring(0, 2048);
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

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank() || "all".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = defaultString(value, "");
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text);
    }

    private boolean isDeleted(Integer value) {
        return value != null && value == 1;
    }

    private String buildSignature(String school, String department, String intro) {
        if (!school.isBlank() && !department.isBlank()) {
            return school + " · " + department;
        }
        if (!school.isBlank()) {
            return school;
        }
        return intro;
    }

    private boolean isProfileIncomplete(String nickname, String school, String department, String intro) {
        return DEFAULT_NICKNAME.equals(nickname)
            || school.isBlank()
            || department.isBlank()
            || intro.isBlank()
            || DEFAULT_SIGNATURE.equals(intro);
    }

    private int normalizeAuthStatus(UserProfile profile) {
        if (profile == null) {
            return 0;
        }
        String verifyType = defaultString(profile.getVerifyType(), "");
        String studentCardImageUrl = defaultString(profile.getStudentCardImageUrl(), "");
        int rawStatus = profile.getAuthStatus() == null ? 0 : profile.getAuthStatus();
        if (!"student_card".equalsIgnoreCase(verifyType) || studentCardImageUrl.isBlank()) {
            return 0;
        }
        return switch (rawStatus) {
            case 1, 2, 3 -> rawStatus;
            default -> 0;
        };
    }

    private String mapAuthLabel(int status) {
        return switch (status) {
            case 1 -> "待审核";
            case 2 -> "已认证";
            case 3 -> "已驳回";
            default -> "未认证";
        };
    }

    private String mapCreditBadge(int creditScore) {
        if (creditScore >= 95) {
            return "高信誉";
        }
        if (creditScore >= 88) {
            return "信誉良好";
        }
        return "普通信誉";
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

    private String mapCondition(Integer condition) {
        int value = condition == null ? 3 : condition;
        return switch (value) {
            case 1 -> "全新";
            case 2 -> "9成新";
            case 3 -> "8成新";
            case 4 -> "7成新";
            default -> "6成新";
        };
    }

    private String mapOrderStatus(Integer status) {
        int value = status == null ? 1 : status;
        return switch (value) {
            case 2 -> "待发货";
            case 3 -> "待收货";
            case 4 -> "已完成";
            case 5 -> "已取消";
            case 6 -> "退款中";
            default -> "待付款";
        };
    }

    private String mapAnnotationType(Integer type) {
        int value = type == null ? 1 : type;
        return switch (value) {
            case 2 -> "疑问";
            case 3 -> "心得";
            default -> "重点";
        };
    }

    private String mapPathDifficulty(Integer difficulty) {
        int value = difficulty == null ? 1 : difficulty;
        return switch (value) {
            case 2 -> "中级";
            case 3 -> "进阶";
            default -> "入门";
        };
    }

    private String mapPathStatus(Integer status) {
        int value = status == null ? 0 : status;
        return switch (value) {
            case 1 -> "已发布";
            case 2 -> "审核中";
            case 3 -> "已下架";
            default -> "草稿";
        };
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FORMATTER.format(time);
    }

    private String resolveBookCover(Book book) {
        String images = defaultString(book.getCoverImages(), "");
        if (!images.isBlank()) {
            String text = images.trim();
            if (text.startsWith("[") && text.endsWith("]")) {
                text = text.substring(1, text.length() - 1);
            }
            String[] parts = text.split(",");
            for (String part : parts) {
                String item = part.replace("\"", "").trim();
                if (!item.isBlank()) {
                    return item;
                }
            }
        }
        return DEFAULT_AVATAR;
    }

    private Map<String, Object> toBookItem(Book book, User seller, UserProfile sellerProfile) {
        Map<String, Object> item = new LinkedHashMap<>();
        String sellerName = seller != null ? defaultString(seller.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME;
        String sellerAvatar = seller != null ? defaultString(seller.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR;
        int sellerScore = sellerProfile != null && sellerProfile.getCreditScore() != null ? sellerProfile.getCreditScore() : 88;
        item.put("id", book.getId());
        item.put("title", defaultString(book.getTitle(), "未命名书籍"));
        item.put("author", defaultString(book.getAuthor(), "未知作者"));
        item.put("publisher", defaultString(book.getPublisher(), ""));
        item.put("isbn", defaultString(book.getIsbn(), ""));
        item.put("price", book.getPrice() == null ? BigDecimal.ZERO : book.getPrice());
        item.put("condition", book.getCondition());
        item.put("conditionLabel", mapCondition(book.getCondition()));
        item.put("annotationCount", book.getAnnotationCount() == null ? 0 : book.getAnnotationCount());
        item.put("resourceCount", 0);
        item.put("status", book.getStatus());
        item.put("statusLabel", mapBookStatus(book.getStatus()));
        item.put("isSold", book.getStatus() != null && book.getStatus() == 3);
        item.put("category", defaultString(book.getCategory(), "教材"));
        item.put("cover", resolveBookCover(book));
        item.put("images", defaultString(book.getCoverImages(), ""));
        item.put("coverImages", defaultString(book.getCoverImages(), ""));
        item.put("sellerId", book.getUserId());
        item.put("sellerName", sellerName);
        item.put("sellerAvatar", sellerAvatar);
        item.put("sellerScore", sellerScore / 20.0);
        item.put("description", defaultString(book.getDescription(), ""));
        item.put("remark", "");
        return item;
    }

    private Map<String, Object> toPathItem(LearningPath path) {
        User creator = path.getUserId() == null ? null : userMapper.selectOneById(path.getUserId());
        long nodeCount = pathNodeMapper.selectCountByQuery(
            QueryWrapper.create().where("path_id = ?", path.getId()).and("is_deleted = 0")
        );
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", path.getId());
        item.put("title", defaultString(path.getTitle(), "学习路径"));
        item.put("description", defaultString(path.getDescription(), ""));
        item.put("creator", creator != null ? defaultString(creator.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        item.put("difficulty", mapPathDifficulty(path.getDifficulty()));
        item.put("totalDuration", (path.getEstimatedHours() == null ? 0 : path.getEstimatedHours()) + "小时");
        item.put("coverImage", defaultString(path.getCoverImage(), ""));
        item.put("status", path.getStatus());
        item.put("statusLabel", mapPathStatus(path.getStatus()));
        item.put("nodeCount", nodeCount);
        return item;
    }

    private Map<String, Object> toNotificationItem(Notification notification) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", notification.getId());
        item.put("type", defaultString(notification.getType(), ""));
        item.put("title", defaultString(notification.getTitle(), ""));
        item.put("content", defaultString(notification.getContent(), ""));
        item.put("routeUrl", defaultString(notification.getRouteUrl(), ""));
        item.put("isRead", notification.getIsRead() != null && notification.getIsRead() == 1);
        item.put("createTime", formatTime(notification.getCreateTime()));
        item.put("updateTime", formatTime(notification.getUpdateTime()));
        return item;
    }

    private Map<String, Object> toBrowseHistoryItem(UserBrowseHistory history) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", history.getId());
        item.put("targetType", defaultString(history.getTargetType(), ""));
        item.put("targetId", history.getTargetId());
        item.put("title", defaultString(history.getTitle(), ""));
        item.put("subTitle", defaultString(history.getSubTitle(), ""));
        item.put("coverUrl", defaultString(history.getCoverUrl(), DEFAULT_AVATAR));
        item.put("routeUrl", defaultString(history.getRouteUrl(), ""));
        item.put("lastViewTime", formatTime(history.getLastViewTime()));
        item.put("createTime", formatTime(history.getCreateTime()));
        return item;
    }

    private Map<String, Object> toAddressItem(UserAddress address) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", address.getId());
        item.put("receiverName", defaultString(address.getReceiverName(), ""));
        item.put("receiverPhone", defaultString(address.getReceiverPhone(), ""));
        item.put("province", defaultString(address.getProvince(), ""));
        item.put("city", defaultString(address.getCity(), ""));
        item.put("district", defaultString(address.getDistrict(), ""));
        item.put("detailAddress", defaultString(address.getDetailAddress(), ""));
        item.put("fullAddress", defaultString(address.getProvince(), "")
            + defaultString(address.getCity(), "")
            + defaultString(address.getDistrict(), "")
            + " "
            + defaultString(address.getDetailAddress(), ""));
        item.put("isDefault", address.getIsDefault() != null && address.getIsDefault() == 1);
        item.put("createTime", formatTime(address.getCreateTime()));
        return item;
    }

    private void clearDefaultAddress(Long userId, Long currentAddressId) {
        List<UserAddress> addresses = userAddressMapper.selectListByQuery(
            QueryWrapper.create().where("user_id = ?", userId).and("is_deleted = 0")
        );
        for (UserAddress address : addresses) {
            if (currentAddressId != null && currentAddressId.equals(address.getId())) {
                continue;
            }
            if (address.getIsDefault() != null && address.getIsDefault() == 1) {
                address.setIsDefault(0);
                address.setUpdateTime(LocalDateTime.now());
                userAddressMapper.update(address);
            }
        }
    }

    private boolean hasDefaultAddress(Long userId) {
        return userAddressMapper.selectCountByQuery(
            QueryWrapper.create().where("user_id = ?", userId).and("is_deleted = 0").and("is_default = 1")
        ) > 0;
    }

    private long countActiveAddresses(Long userId) {
        return userAddressMapper.selectCountByQuery(
            QueryWrapper.create().where("user_id = ?", userId).and("is_deleted = 0")
        );
    }

    private Map<Long, Book> loadBooksByIds(List<Long> ids) {
        List<Long> uniqueIds = ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (uniqueIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = uniqueIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Book> books = bookMapper.selectListByQuery(
            QueryWrapper.create().where("id in (" + placeholders + ")", uniqueIds.toArray())
        );
        Map<Long, Book> result = new LinkedHashMap<>();
        for (Book book : books) {
            result.put(book.getId(), book);
        }
        return result;
    }

    private Map<Long, User> loadUsersByIds(List<Long> ids) {
        List<Long> uniqueIds = ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (uniqueIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = uniqueIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<User> users = userMapper.selectListByQuery(
            QueryWrapper.create().where("id in (" + placeholders + ")", uniqueIds.toArray())
        );
        Map<Long, User> result = new LinkedHashMap<>();
        for (User user : users) {
            result.put(user.getId(), user);
        }
        return result;
    }
}
