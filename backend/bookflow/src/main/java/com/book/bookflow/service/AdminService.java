package com.book.bookflow.service;

import com.alibaba.fastjson.JSON;
import com.book.bookflow.common.auth.AdminAuthContext;
import com.book.bookflow.common.utils.AdminPasswordUtil;
import com.book.bookflow.common.utils.JwtUtil;
import com.book.bookflow.entity.AdminOperationLog;
import com.book.bookflow.entity.AdminUser;
import com.book.bookflow.entity.Banner;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.Comment;
import com.book.bookflow.entity.ContentReport;
import com.book.bookflow.entity.LearningPath;
import com.book.bookflow.entity.Notification;
import com.book.bookflow.entity.OrderIssue;
import com.book.bookflow.entity.PathNode;
import com.book.bookflow.entity.Post;
import com.book.bookflow.entity.Resource;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserFeedback;
import com.book.bookflow.entity.UserProfile;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.AdminOperationLogMapper;
import com.book.bookflow.mapper.AdminUserMapper;
import com.book.bookflow.mapper.BannerMapper;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.CommentMapper;
import com.book.bookflow.mapper.ContentReportMapper;
import com.book.bookflow.mapper.LearningPathMapper;
import com.book.bookflow.mapper.NotificationMapper;
import com.book.bookflow.mapper.OrderIssueMapper;
import com.book.bookflow.mapper.PathNodeMapper;
import com.book.bookflow.mapper.PostMapper;
import com.book.bookflow.mapper.ResourceMapper;
import com.book.bookflow.mapper.UserFeedbackMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.mapper.UserProfileMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final long ADMIN_TOKEN_EXPIRE_MILLIS = 12L * 60 * 60 * 1000;
    private static final long MAX_ADMIN_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final String DEFAULT_AVATAR = "/static/logo.png";

    private final AdminUserMapper adminUserMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final BookMapper bookMapper;
    private final BookOrderMapper bookOrderMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final ContentReportMapper contentReportMapper;
    private final UserFeedbackMapper userFeedbackMapper;
    private final ResourceMapper resourceMapper;
    private final LearningPathMapper learningPathMapper;
    private final PathNodeMapper pathNodeMapper;
    private final OrderIssueMapper orderIssueMapper;
    private final NotificationMapper notificationMapper;
    private final BannerMapper bannerMapper;

    @Value("${app.image-base-url:}")
    private String imageBaseUrl;

    public AdminService(
        AdminUserMapper adminUserMapper,
        AdminOperationLogMapper adminOperationLogMapper,
        UserMapper userMapper,
        UserProfileMapper userProfileMapper,
        BookMapper bookMapper,
        BookOrderMapper bookOrderMapper,
        PostMapper postMapper,
        CommentMapper commentMapper,
        ContentReportMapper contentReportMapper,
        UserFeedbackMapper userFeedbackMapper,
        ResourceMapper resourceMapper,
        LearningPathMapper learningPathMapper,
        PathNodeMapper pathNodeMapper,
        OrderIssueMapper orderIssueMapper,
        NotificationMapper notificationMapper,
        BannerMapper bannerMapper
    ) {
        this.adminUserMapper = adminUserMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.bookMapper = bookMapper;
        this.bookOrderMapper = bookOrderMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.contentReportMapper = contentReportMapper;
        this.userFeedbackMapper = userFeedbackMapper;
        this.resourceMapper = resourceMapper;
        this.learningPathMapper = learningPathMapper;
        this.pathNodeMapper = pathNodeMapper;
        this.orderIssueMapper = orderIssueMapper;
        this.notificationMapper = notificationMapper;
        this.bannerMapper = bannerMapper;
    }

    public Map<String, Object> login(Map<String, Object> payload) {
        String username = text(payload.get("username"));
        String password = text(payload.get("password"));
        if (username.isBlank() || password.isBlank()) {
            throw new CustomerException("400", "请输入管理员账号和密码");
        }
        AdminUser adminUser = adminUserMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("username = ?", username)
                .and("is_deleted = 0")
                .limit(1)
        );
        if (adminUser == null || !AdminPasswordUtil.matches(username, password, adminUser.getPasswordHash())) {
            throw new CustomerException("401", "管理员账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(adminUser.getStatus())) {
            throw new CustomerException("403", "管理员账号已禁用");
        }

        if (AdminPasswordUtil.needsRehash(adminUser.getPasswordHash())) {
            adminUser.setPasswordHash(AdminPasswordUtil.hashPassword(username, password));
        }
        adminUser.setLastLoginTime(LocalDateTime.now());
        adminUser.setUpdateTime(LocalDateTime.now());
        adminUserMapper.update(adminUser);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("type", "admin");
        claims.put("adminId", adminUser.getId());
        claims.put("username", adminUser.getUsername());
        claims.put("role", adminUser.getRole());
        claims.put("timestamp", System.currentTimeMillis());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", JwtUtil.genToken(claims, ADMIN_TOKEN_EXPIRE_MILLIS));
        return result;
    }

    public Map<String, Object> refreshToken(String authorization) {
        String token = normalizeToken(authorization);
        if (token.isBlank()) {
            throw new CustomerException("401", "请先登录后台");
        }
        Map<String, Object> claims = JwtUtil.parseToken(token);
        if (!"admin".equals(String.valueOf(claims.get("type")))) {
            throw new CustomerException("401", "身份类型不正确");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", JwtUtil.genToken(claims, ADMIN_TOKEN_EXPIRE_MILLIS));
        result.put("status", 0);
        return result;
    }

    public Map<String, Object> getAdminInfo() {
        AdminUser adminUser = currentAdmin();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", adminUser.getId());
        result.put("username", adminUser.getUsername());
        result.put("realName", defaultString(adminUser.getRealName(), adminUser.getUsername()));
        result.put("avatar", DEFAULT_AVATAR);
        result.put("roles", List.of(defaultString(adminUser.getRole(), "admin")));
        result.put("desc", "BookFlow 后台管理员");
        result.put("homePath", "/bookflow/dashboard");
        result.put("token", defaultString(AdminAuthContext.getToken(), ""));
        return result;
    }

    public List<String> getAccessCodes() {
        return List.of("BOOKFLOW_ADMIN");
    }

    public List<Map<String, Object>> getMenus() {
        Map<String, Object> root = route("BookFlowAdmin", "/bookflow", null, "BookFlow后台", "lucide:book-open");
        root.put("redirect", "/bookflow/dashboard");
        List<Map<String, Object>> children = new ArrayList<>();
        children.add(route("BookFlowDashboard", "/bookflow/dashboard", "/bookflow/dashboard/index", "数据概览", "lucide:layout-dashboard"));
        children.add(route("BookFlowUsers", "/bookflow/users", "/bookflow/users/index", "用户管理", "lucide:users"));
        children.add(route("BookFlowVerify", "/bookflow/verify", "/bookflow/verify/index", "认证审核", "lucide:badge-check"));
        children.add(route("BookFlowBooks", "/bookflow/books", "/bookflow/books/index", "书籍管理", "lucide:book-marked"));
        children.add(route("BookFlowOrders", "/bookflow/orders", "/bookflow/orders/index", "订单管理", "lucide:receipt-text"));
        children.add(route("BookFlowPosts", "/bookflow/posts", "/bookflow/posts/index", "内容管理", "lucide:message-square-text"));
        children.add(route("BookFlowReports", "/bookflow/reports", "/bookflow/reports/index", "举报审核", "lucide:shield-alert"));
        children.add(route("BookFlowFeedbacks", "/bookflow/feedbacks", "/bookflow/feedbacks/index", "反馈管理", "lucide:inbox"));
        children.add(route("BookFlowResources", "/bookflow/resources", "/bookflow/resources/index", "资源管理", "lucide:file-stack"));
        children.add(route("BookFlowPaths", "/bookflow/paths", "/bookflow/paths/index", "学习路径", "lucide:route"));
        children.add(route("BookFlowBanners", "/bookflow/banners", "/bookflow/banners/index", "Banner管理", "lucide:images"));
        children.add(route("BookFlowLogs", "/bookflow/logs", "/bookflow/logs/index", "系统日志", "lucide:scroll-text"));
        root.put("children", children);
        return List.of(root);
    }

    public Map<String, Object> dashboardSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", userMapper.selectCountByQuery(QueryWrapper.create().where("is_deleted = 0")));
        result.put("pendingVerifyCount", userProfileMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("auth_status = 1")
        ));
        result.put("sellingBookCount", bookMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("status = 1")
        ));
        result.put("orderCount", bookOrderMapper.selectCountByQuery(QueryWrapper.create().where("is_deleted = 0")));
        result.put("pendingReportCount", contentReportMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("status = 0")
        ));
        result.put("pendingFeedbackCount", userFeedbackMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("status = 0")
        ));
        result.put("todayUserCount", userMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("create_time >= ?", todayStart)
        ));
        result.put("todayBookCount", bookMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("create_time >= ?", todayStart)
        ));
        result.put("todayOrderCount", bookOrderMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("create_time >= ?", todayStart)
        ));
        result.put("todayPostCount", postMapper.selectCountByQuery(
            QueryWrapper.create().where("is_deleted = 0").and("create_time >= ?", todayStart)
        ));
        return result;
    }

    public Map<String, Object> listUsers(Integer pageNo, Integer pageSize, String keyword, Integer status, Integer authStatus) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        if (status != null) {
            wrapper.and("is_deleted = ?", status == 1 ? 0 : 1);
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            String like = "%" + normalizedKeyword + "%";
            wrapper.and("(nickname LIKE ? OR mobile LIKE ? OR openid LIKE ?)", like, like, like);
        }
        if (authStatus != null) {
            List<UserProfile> profiles = userProfileMapper.selectListByQuery(
                QueryWrapper.create().where("is_deleted = 0").and("auth_status = ?", authStatus)
            );
            List<Long> userIds = profiles.stream().map(UserProfile::getUserId).filter(Objects::nonNull).distinct().toList();
            if (userIds.isEmpty()) {
                return pageResult(List.of(), 0);
            }
            wrapper.and("id in (" + placeholders(userIds.size()) + ")", userIds.toArray());
        }
        long total = userMapper.selectCountByQuery(wrapper);
        List<User> users = userMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(users.stream().map(this::toUserItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveUser(Map<String, Object> payload) {
        Long userId = asLong(payload.get("id"));
        boolean creating = userId == null;
        User user = creating ? new User() : requireUser(userId);
        String before = creating ? null : JSON.toJSONString(user);
        LocalDateTime now = LocalDateTime.now();

        user.setOpenid(defaultString(text(payload.get("openid")), defaultString(user.getOpenid(), "admin_" + UUID.randomUUID().toString().replace("-", ""))));
        user.setNickname(defaultString(text(payload.get("nickname")), defaultString(user.getNickname(), "后台用户")));
        user.setAvatarUrl(defaultString(text(payload.get("avatarUrl")), defaultString(user.getAvatarUrl(), DEFAULT_AVATAR)));
        user.setMobile(text(payload.get("mobile")));
        user.setGender(asInteger(payload.get("gender"), user.getGender() == null ? 0 : user.getGender()));
        user.setProvince(text(payload.get("province")));
        user.setCity(text(payload.get("city")));
        user.setUpdateTime(now);
        user.setIsDeleted(flagEnabled(payload.get("enabled"), !Integer.valueOf(1).equals(user.getIsDeleted())) ? 0 : 1);
        if (creating) {
            user.setCreateTime(now);
            userMapper.insert(user);
            userId = user.getId();
        } else {
            userMapper.update(user);
        }

        UserProfile profile = getOrCreateProfile(userId);
        String profileBefore = JSON.toJSONString(profile);
        fillProfile(profile, payload, now);
        userProfileMapper.update(profile);
        logOperation("user", creating ? "create" : "update", user.getId(), before, JSON.toJSONString(Map.of("user", user, "profile", profile)));
        if (!Objects.equals(profileBefore, JSON.toJSONString(profile))) {
            logOperation("verify", "save_profile", profile.getUserId(), profileBefore, JSON.toJSONString(profile));
        }
        return toUserItem(user);
    }

    @Transactional
    public Map<String, Object> changeUserStatus(Map<String, Object> payload) {
        Long userId = asLong(payload.get("userId"));
        Integer enabled = asInteger(payload.get("enabled"), null);
        User user = requireUser(userId);
        String before = JSON.toJSONString(user);
        user.setIsDeleted(Integer.valueOf(1).equals(enabled) ? 0 : 1);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
        logOperation("user", Integer.valueOf(1).equals(enabled) ? "enable" : "disable", userId, before, JSON.toJSONString(user));
        return toUserItem(user);
    }

    @Transactional
    public Map<String, Object> updateUserCredit(Map<String, Object> payload) {
        Long userId = asLong(payload.get("userId"));
        Integer creditScore = asInteger(payload.get("creditScore"), null);
        if (creditScore == null || creditScore < 0 || creditScore > 100) {
            throw new CustomerException("400", "信用分必须在 0-100 之间");
        }
        UserProfile profile = getOrCreateProfile(userId);
        String before = JSON.toJSONString(profile);
        profile.setCreditScore(creditScore);
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.update(profile);
        logOperation("user", "update_credit", userId, before, JSON.toJSONString(profile));
        return toProfileItem(profile);
    }

    public Map<String, Object> listVerifies(Integer pageNo, Integer pageSize, String keyword, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("is_deleted = 0");
        if (status != null) {
            wrapper.and("auth_status = ?", status);
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            String like = "%" + normalizedKeyword + "%";
            wrapper.and("(real_name LIKE ? OR student_id LIKE ? OR school LIKE ? OR department LIKE ?)", like, like, like, like);
        }
        long total = userProfileMapper.selectCountByQuery(wrapper);
        List<UserProfile> profiles = userProfileMapper.selectListByQuery(page(
            wrapper.orderBy("case when auth_status = 1 then 0 else 1 end asc, update_time desc, id desc"),
            pageNo,
            pageSize
        ));
        return pageResult(profiles.stream().map(this::toProfileItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveVerify(Map<String, Object> payload) {
        Long profileId = asLong(payload.get("id"));
        UserProfile profile;
        if (profileId == null) {
            Long userId = asLong(payload.get("userId"));
            if (userId == null) {
                userId = createAdminManagedUser(defaultString(text(payload.get("nickname")), "认证用户"));
            }
            profile = getOrCreateProfile(userId);
        } else {
            profile = userProfileMapper.selectOneById(profileId);
            if (profile == null) {
                throw new CustomerException("404", "认证资料不存在");
            }
        }

        User user = requireUser(profile.getUserId());
        String before = JSON.toJSONString(Map.of("user", user, "profile", profile));
        LocalDateTime now = LocalDateTime.now();
        String nickname = text(payload.get("nickname"));
        if (!nickname.isBlank()) {
            user.setNickname(nickname);
        }
        String avatarUrl = text(payload.get("avatarUrl"));
        if (!avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        String mobile = text(payload.get("mobile"));
        if (!mobile.isBlank()) {
            user.setMobile(mobile);
        }
        user.setUpdateTime(now);
        userMapper.update(user);

        fillProfile(profile, payload, now);
        userProfileMapper.update(profile);
        logOperation("verify", profileId == null ? "create" : "update", profile.getUserId(), before, JSON.toJSONString(Map.of("user", user, "profile", profile)));
        return toProfileItem(profile);
    }

    @Transactional
    public Map<String, Object> auditVerify(Map<String, Object> payload) {
        Long userId = asLong(payload.get("userId"));
        Integer status = asInteger(payload.get("status"), null);
        String remark = text(payload.get("auditRemark"));
        if (!List.of(2, 3).contains(status)) {
            throw new CustomerException("400", "审核状态只能是通过或驳回");
        }
        UserProfile profile = getOrCreateProfile(userId);
        String before = JSON.toJSONString(profile);
        profile.setAuthStatus(status);
        profile.setAuditRemark(remark);
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.update(profile);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType("verify");
        notification.setTitle(status == 2 ? "学生认证已通过" : "学生认证未通过");
        notification.setContent(status == 2 ? "你的学生身份认证已通过审核。" : defaultString(remark, "你的学生身份认证未通过，请补充资料后重新提交。"));
        notification.setRouteUrl("/pages/my/my");
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        notification.setIsDeleted(0);
        notificationMapper.insert(notification);

        logOperation("verify", status == 2 ? "approve" : "reject", userId, before, JSON.toJSONString(profile));
        return toProfileItem(profile);
    }

    public Map<String, Object> listBooks(Integer pageNo, Integer pageSize, String keyword, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        if (status != null) {
            wrapper.and("status = ?", status);
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            String like = "%" + normalizedKeyword + "%";
            wrapper.and("(title LIKE ? OR author LIKE ? OR publisher LIKE ? OR isbn LIKE ? OR category LIKE ?)", like, like, like, like, like);
        }
        long total = bookMapper.selectCountByQuery(wrapper);
        List<Book> books = bookMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(books.stream().map(this::toBookItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveBook(Map<String, Object> payload) {
        Long bookId = asLong(payload.get("id"));
        boolean creating = bookId == null;
        Book book = creating ? new Book() : requireBook(bookId);
        String before = creating ? null : JSON.toJSONString(book);
        LocalDateTime now = LocalDateTime.now();

        Long userId = asLong(payload.get("userId"));
        if (userId != null) {
            requireUser(userId);
            book.setUserId(userId);
        } else if (creating) {
            book.setUserId(createAdminManagedUser("后台卖家"));
        }
        book.setTitle(defaultString(text(payload.get("title")), defaultString(book.getTitle(), "未命名书籍")));
        book.setAuthor(text(payload.get("author")));
        book.setPublisher(text(payload.get("publisher")));
        book.setIsbn(text(payload.get("isbn")));
        book.setCategory(text(payload.get("category")));
        book.setDescription(text(payload.get("description")));
        book.setCondition(asInteger(payload.get("condition"), book.getCondition() == null ? 3 : book.getCondition()));
        book.setPrice(asBigDecimal(payload.get("price"), book.getPrice() == null ? BigDecimal.ZERO : book.getPrice()));
        book.setOriginalPrice(asBigDecimal(payload.get("originalPrice"), book.getOriginalPrice()));
        book.setStatus(asInteger(payload.get("status"), book.getStatus() == null ? 1 : book.getStatus()));
        book.setViewCount(asInteger(payload.get("viewCount"), book.getViewCount() == null ? 0 : book.getViewCount()));
        book.setFavoriteCount(asInteger(payload.get("favoriteCount"), book.getFavoriteCount() == null ? 0 : book.getFavoriteCount()));
        book.setAnnotationCount(asInteger(payload.get("annotationCount"), book.getAnnotationCount() == null ? 0 : book.getAnnotationCount()));
        if (payload.containsKey("images") || payload.containsKey("coverImages")) {
            book.setCoverImages(String.join(",", normalizeImagePayload(payload.containsKey("images") ? payload.get("images") : payload.get("coverImages"))));
        } else if (creating) {
            book.setCoverImages("");
        }
        book.setUpdateTime(now);
        book.setIsDeleted(asInteger(payload.get("isDeleted"), book.getIsDeleted() == null ? 0 : book.getIsDeleted()));
        if (creating) {
            book.setCreateTime(now);
            bookMapper.insert(book);
        } else {
            bookMapper.update(book);
        }
        logOperation("book", creating ? "create" : "update", book.getId(), before, JSON.toJSONString(book));
        return toBookItem(book);
    }

    @Transactional
    public Map<String, Object> changeBookStatus(Map<String, Object> payload) {
        Long bookId = asLong(payload.get("bookId"));
        Integer status = asInteger(payload.get("status"), null);
        if (!List.of(1, 2, 3, 4).contains(status)) {
            throw new CustomerException("400", "书籍状态不正确");
        }
        Book book = requireBook(bookId);
        String before = JSON.toJSONString(book);
        book.setStatus(status);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.update(book);
        logOperation("book", "change_status", bookId, before, JSON.toJSONString(book));
        return toBookItem(book);
    }

    @Transactional
    public Map<String, Object> deleteBook(Map<String, Object> payload) {
        Long bookId = asLong(payload.get("bookId"));
        Book book = requireBook(bookId);
        String before = JSON.toJSONString(book);
        book.setIsDeleted(1);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.update(book);
        logOperation("book", "delete", bookId, before, JSON.toJSONString(book));
        return toBookItem(book);
    }

    public Map<String, Object> uploadBookImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomerException("400", "请选择书籍图片");
        }
        String extension = resolveImageExtension(file.getOriginalFilename());
        String fileName = "admin_" + AdminAuthContext.requireAdminId() + "_" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get("uploads", "book");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CustomerException("500", "书籍图片上传失败");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", normalizeBaseUrl(imageBaseUrl) + "uploads/book/" + fileName);
        result.put("fileName", fileName);
        return result;
    }

    public Map<String, Object> uploadBookImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new CustomerException("400", "请选择书籍图片");
        }
        List<Map<String, Object>> items = Arrays.stream(files)
            .filter(file -> file != null && !file.isEmpty())
            .map(this::uploadBookImage)
            .toList();
        if (items.isEmpty()) {
            throw new CustomerException("400", "请选择书籍图片");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("urls", items.stream().map(item -> item.get("url")).toList());
        return result;
    }

    public Map<String, Object> uploadStudentCardImage(MultipartFile file) {
        validateAdminImageFile(file, "学生证图片");
        String extension = resolveAllowedImageExtension(file.getOriginalFilename());
        String fileName = "admin_student_card_" + AdminAuthContext.requireAdminId() + "_" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get("uploads", "profile", "student-card");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CustomerException("500", "学生证图片上传失败");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", normalizeBaseUrl(imageBaseUrl) + "uploads/profile/student-card/" + fileName);
        result.put("fileName", fileName);
        return result;
    }

    public Path resolveStudentCardImagePath(String rawUrl) {
        String fileName = extractStudentCardFileName(rawUrl);
        if (fileName.isBlank()) {
            throw new CustomerException("400", "学生证图片地址错误");
        }
        Path directory = Paths.get("uploads", "profile", "student-card").toAbsolutePath().normalize();
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory) || !Files.isRegularFile(target)) {
            throw new CustomerException("404", "学生证图片不存在");
        }
        return target;
    }

    public Map<String, Object> uploadPathCoverImage(MultipartFile file) {
        validateAdminImageFile(file, "路径封面图");
        String extension = resolveAllowedImageExtension(file.getOriginalFilename());
        String fileName = "admin_path_cover_" + AdminAuthContext.requireAdminId() + "_" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get("uploads", "path");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CustomerException("500", "路径封面图上传失败");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", normalizeBaseUrl(imageBaseUrl) + "uploads/path/" + fileName);
        result.put("fileName", fileName);
        result.put("auditStatus", 2);
        result.put("auditStatusLabel", "已通过");
        return result;
    }

    @Transactional
    public Map<String, Object> updateBookImages(Map<String, Object> payload) {
        Long bookId = asLong(payload.get("bookId"));
        Book book = requireBook(bookId);
        List<String> images = normalizeImagePayload(payload.get("images"));
        if (images.isEmpty()) {
            throw new CustomerException("400", "请至少上传一张书籍图片");
        }
        if (images.size() > 9) {
            throw new CustomerException("400", "书籍图片最多上传9张");
        }
        String before = JSON.toJSONString(book);
        book.setCoverImages(String.join(",", images));
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.update(book);
        logOperation("book", "update_images", bookId, before, JSON.toJSONString(book));
        return toBookItem(book);
    }

    public Map<String, Object> listOrders(Integer pageNo, Integer pageSize, String keyword, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("is_deleted = 0");
        if (status != null) {
            wrapper.and("status = ?", status);
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            String like = "%" + normalizedKeyword + "%";
            wrapper.and("(order_no LIKE ? OR receiver_name LIKE ? OR receiver_phone LIKE ?)", like, like, like);
        }
        long total = bookOrderMapper.selectCountByQuery(wrapper);
        List<BookOrder> orders = bookOrderMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(orders.stream().map(this::toOrderItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveOrder(Map<String, Object> payload) {
        Long orderId = asLong(payload.get("id"));
        boolean creating = orderId == null;
        BookOrder order = creating ? new BookOrder() : bookOrderMapper.selectOneById(orderId);
        if (order == null) {
            throw new CustomerException("404", "订单不存在");
        }
        String before = creating ? null : JSON.toJSONString(order);
        LocalDateTime now = LocalDateTime.now();

        Long bookId = asLong(payload.get("bookId"));
        Book book = bookId == null ? null : requireBook(bookId);
        order.setBookId(bookId);
        Long buyerId = asLong(payload.get("buyerId"));
        if (buyerId != null) {
            requireUser(buyerId);
        } else if (creating) {
            buyerId = createAdminManagedUser("后台买家");
        }
        order.setBuyerId(buyerId);
        Long sellerId = asLong(payload.get("sellerId"));
        if (sellerId != null) {
            requireUser(sellerId);
        } else if (book != null) {
            sellerId = book.getUserId();
        } else if (creating) {
            sellerId = createAdminManagedUser("后台卖家");
        }
        order.setSellerId(sellerId);
        order.setOrderNo(defaultString(text(payload.get("orderNo")), defaultString(order.getOrderNo(), "ADM" + System.currentTimeMillis())));
        order.setTotalAmount(asBigDecimal(payload.get("totalAmount"), order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount()));
        order.setStatus(asInteger(payload.get("status"), order.getStatus() == null ? 0 : order.getStatus()));
        order.setPaymentMethod(asInteger(payload.get("paymentMethod"), order.getPaymentMethod() == null ? 0 : order.getPaymentMethod()));
        order.setBuyerMessage(text(payload.get("buyerMessage")));
        order.setReceiverName(text(payload.get("receiverName")));
        order.setReceiverPhone(text(payload.get("receiverPhone")));
        order.setReceiverAddress(text(payload.get("receiverAddress")));
        order.setUpdateTime(now);
        order.setIsDeleted(asInteger(payload.get("isDeleted"), order.getIsDeleted() == null ? 0 : order.getIsDeleted()));
        if (creating) {
            order.setCreateTime(now);
            bookOrderMapper.insert(order);
        } else {
            bookOrderMapper.update(order);
        }
        logOperation("order", creating ? "create" : "update", order.getId(), before, JSON.toJSONString(order));
        return toOrderItem(order);
    }

    public List<Map<String, Object>> listOrderIssues(Long orderId) {
        return orderIssueMapper.selectListByQuery(
            QueryWrapper.create().where("order_id = ?", orderId).and("is_deleted = 0").orderBy("id desc")
        ).stream().map(this::toOrderIssueItem).toList();
    }

    public Map<String, Object> listPosts(Integer pageNo, Integer pageSize, String keyword, Integer type, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        if (type != null) {
            wrapper.and("type = ?", type);
        }
        if (status != null) {
            wrapper.and("is_deleted = ?", status == 1 ? 0 : 1);
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            String like = "%" + normalizedKeyword + "%";
            wrapper.and("(title LIKE ? OR content LIKE ?)", like, like);
        }
        long total = postMapper.selectCountByQuery(wrapper);
        List<Post> posts = postMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(posts.stream().map(this::toPostItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> savePost(Map<String, Object> payload) {
        Long postId = asLong(payload.get("id"));
        boolean creating = postId == null;
        Post post = creating ? new Post() : requirePost(postId);
        String before = creating ? null : JSON.toJSONString(post);
        LocalDateTime now = LocalDateTime.now();

        Long userId = asLong(payload.get("userId"));
        if (userId != null) {
            requireUser(userId);
        } else if (creating) {
            userId = createAdminManagedUser("后台发帖用户");
        }
        post.setUserId(userId);
        post.setTitle(defaultString(text(payload.get("title")), defaultString(post.getTitle(), "后台帖子")));
        post.setContent(defaultString(text(payload.get("content")), defaultString(post.getContent(), "")));
        post.setType(asInteger(payload.get("type"), post.getType() == null ? 0 : post.getType()));
        post.setSharedPathId(asLong(payload.get("sharedPathId")));
        post.setViewCount(asInteger(payload.get("viewCount"), post.getViewCount() == null ? 0 : post.getViewCount()));
        post.setLikeCount(asInteger(payload.get("likeCount"), post.getLikeCount() == null ? 0 : post.getLikeCount()));
        post.setCommentCount(asInteger(payload.get("commentCount"), post.getCommentCount() == null ? 0 : post.getCommentCount()));
        post.setIsDeleted(visibleToIsDeleted(payload.get("visible"), post.getIsDeleted() == null ? 0 : post.getIsDeleted()));
        post.setUpdateTime(now);
        if (creating) {
            post.setCreateTime(now);
            postMapper.insert(post);
        } else {
            postMapper.update(post);
        }
        logOperation("post", creating ? "create" : "update", post.getId(), before, JSON.toJSONString(post));
        return toPostItem(post);
    }

    @Transactional
    public Map<String, Object> changePostStatus(Map<String, Object> payload) {
        Long postId = asLong(payload.get("postId"));
        Integer visible = asInteger(payload.get("visible"), null);
        Post post = requirePost(postId);
        String before = JSON.toJSONString(post);
        post.setIsDeleted(Integer.valueOf(1).equals(visible) ? 0 : 1);
        post.setUpdateTime(LocalDateTime.now());
        postMapper.update(post);
        logOperation("post", Integer.valueOf(1).equals(visible) ? "restore" : "hide", postId, before, JSON.toJSONString(post));
        return toPostItem(post);
    }

    public Map<String, Object> listComments(Integer pageNo, Integer pageSize, String keyword, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        if (status != null) {
            wrapper.and("is_deleted = ?", status == 1 ? 0 : 1);
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            wrapper.and("content LIKE ?", "%" + normalizedKeyword + "%");
        }
        long total = commentMapper.selectCountByQuery(wrapper);
        List<Comment> comments = commentMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(comments.stream().map(this::toCommentItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> changeCommentStatus(Map<String, Object> payload) {
        Long commentId = asLong(payload.get("commentId"));
        Integer visible = asInteger(payload.get("visible"), null);
        Comment comment = requireComment(commentId);
        String before = JSON.toJSONString(comment);
        comment.setIsDeleted(Integer.valueOf(1).equals(visible) ? 0 : 1);
        commentMapper.update(comment);
        logOperation("comment", Integer.valueOf(1).equals(visible) ? "restore" : "hide", commentId, before, JSON.toJSONString(comment));
        return toCommentItem(comment);
    }

    public Map<String, Object> listReports(Integer pageNo, Integer pageSize, String targetType, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("is_deleted = 0");
        if (status != null) {
            wrapper.and("status = ?", status);
        }
        String normalizedTargetType = text(targetType);
        if (!normalizedTargetType.isBlank()) {
            wrapper.and("target_type = ?", normalizedTargetType);
        }
        long total = contentReportMapper.selectCountByQuery(wrapper);
        List<ContentReport> reports = contentReportMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(reports.stream().map(this::toReportItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveReport(Map<String, Object> payload) {
        Long reportId = asLong(payload.get("id"));
        boolean creating = reportId == null;
        ContentReport report = creating ? new ContentReport() : requireReport(reportId);
        String before = creating ? null : JSON.toJSONString(report);
        LocalDateTime now = LocalDateTime.now();

        Long userId = asLong(payload.get("userId"));
        if (userId != null) {
            requireUser(userId);
        } else if (creating) {
            userId = createAdminManagedUser("后台举报用户");
        }
        report.setUserId(userId);
        report.setTargetType(defaultString(text(payload.get("targetType")), defaultString(report.getTargetType(), "other")));
        report.setTargetId(asLong(payload.get("targetId")) == null ? (report.getTargetId() == null ? 0L : report.getTargetId()) : asLong(payload.get("targetId")));
        report.setReasonType(defaultString(text(payload.get("reasonType")), defaultString(report.getReasonType(), "后台录入")));
        report.setContent(defaultString(text(payload.get("content")), defaultString(report.getContent(), "")));
        report.setStatus(asInteger(payload.get("status"), report.getStatus() == null ? 0 : report.getStatus()));
        report.setUpdateTime(now);
        report.setIsDeleted(asInteger(payload.get("isDeleted"), report.getIsDeleted() == null ? 0 : report.getIsDeleted()));
        if (creating) {
            report.setCreateTime(now);
            contentReportMapper.insert(report);
        } else {
            contentReportMapper.update(report);
        }
        logOperation("report", creating ? "create" : "update", report.getId(), before, JSON.toJSONString(report));
        return toReportItem(report);
    }

    @Transactional
    public Map<String, Object> handleReport(Map<String, Object> payload) {
        Long reportId = asLong(payload.get("reportId"));
        Integer status = asInteger(payload.get("status"), 1);
        boolean hideTarget = Boolean.TRUE.equals(payload.get("hideTarget")) || Integer.valueOf(1).equals(asInteger(payload.get("hideTarget"), 0));
        if (!List.of(1, 2).contains(status)) {
            throw new CustomerException("400", "举报处理状态不正确");
        }
        ContentReport report = requireReport(reportId);
        String before = JSON.toJSONString(report);
        report.setStatus(status);
        report.setUpdateTime(LocalDateTime.now());
        contentReportMapper.update(report);
        if (hideTarget) {
            hideReportedTarget(report);
        }
        logOperation("report", hideTarget ? "handle_and_hide" : "handle", reportId, before, JSON.toJSONString(report));
        return toReportItem(report);
    }

    public Map<String, Object> listFeedbacks(Integer pageNo, Integer pageSize, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("is_deleted = 0");
        if (status != null) {
            wrapper.and("status = ?", status);
        }
        long total = userFeedbackMapper.selectCountByQuery(wrapper);
        List<UserFeedback> feedbacks = userFeedbackMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(feedbacks.stream().map(this::toFeedbackItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveFeedback(Map<String, Object> payload) {
        Long feedbackId = asLong(payload.get("id"));
        boolean creating = feedbackId == null;
        UserFeedback feedback = creating ? new UserFeedback() : requireFeedback(feedbackId);
        String before = creating ? null : JSON.toJSONString(feedback);
        LocalDateTime now = LocalDateTime.now();

        Long userId = asLong(payload.get("userId"));
        if (userId != null) {
            requireUser(userId);
        } else if (creating) {
            userId = createAdminManagedUser("后台反馈用户");
        }
        feedback.setUserId(userId);
        feedback.setFeedbackType(defaultString(text(payload.get("feedbackType")), defaultString(feedback.getFeedbackType(), "other")));
        feedback.setContent(defaultString(text(payload.get("content")), defaultString(feedback.getContent(), "")));
        feedback.setContact(text(payload.get("contact")));
        feedback.setPagePath(text(payload.get("pagePath")));
        feedback.setStatus(asInteger(payload.get("status"), feedback.getStatus() == null ? 0 : feedback.getStatus()));
        feedback.setUpdateTime(now);
        feedback.setIsDeleted(asInteger(payload.get("isDeleted"), feedback.getIsDeleted() == null ? 0 : feedback.getIsDeleted()));
        if (creating) {
            feedback.setCreateTime(now);
            userFeedbackMapper.insert(feedback);
        } else {
            userFeedbackMapper.update(feedback);
        }
        logOperation("feedback", creating ? "create" : "update", feedback.getId(), before, JSON.toJSONString(feedback));
        return toFeedbackItem(feedback);
    }

    @Transactional
    public Map<String, Object> handleFeedback(Map<String, Object> payload) {
        Long feedbackId = asLong(payload.get("feedbackId"));
        UserFeedback feedback = requireFeedback(feedbackId);
        String before = JSON.toJSONString(feedback);
        feedback.setStatus(1);
        feedback.setUpdateTime(LocalDateTime.now());
        userFeedbackMapper.update(feedback);
        logOperation("feedback", "handle", feedbackId, before, JSON.toJSONString(feedback));
        return toFeedbackItem(feedback);
    }

    public Map<String, Object> listResources(Integer pageNo, Integer pageSize, String keyword, Integer status, Integer visibility, String bindType) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        if (status != null) {
            wrapper.and("is_deleted = ?", status == 1 ? 0 : 1);
        }
        if (visibility != null) {
            wrapper.and("visibility = ?", visibility);
        }
        String normalizedBindType = text(bindType);
        if (!normalizedBindType.isBlank()) {
            if ("none".equals(normalizedBindType)) {
                wrapper.and("(bind_type is null or bind_type = '' or bind_type = 'none')");
            } else {
                wrapper.and("bind_type = ?", normalizedBindType);
            }
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            String like = "%" + normalizedKeyword + "%";
            wrapper.and("(title LIKE ? OR description LIKE ? OR file_format LIKE ?)", like, like, like);
        }
        long total = resourceMapper.selectCountByQuery(wrapper);
        List<Resource> resources = resourceMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(resources.stream().map(this::toResourceItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveResource(Map<String, Object> payload) {
        Long resourceId = asLong(payload.get("id"));
        boolean creating = resourceId == null;
        Resource resource = creating ? new Resource() : requireResource(resourceId);
        String before = creating ? null : JSON.toJSONString(resource);
        LocalDateTime now = LocalDateTime.now();

        Long userId = asLong(payload.get("userId"));
        if (userId != null) {
            requireUser(userId);
        } else if (creating) {
            userId = createAdminManagedUser("后台资源用户");
        }
        Long bookId = asLong(payload.get("bookId"));
        String bindType = defaultString(text(payload.get("bindType")), bookId == null ? defaultString(resource.getBindType(), "none") : "book");
        Long bindId = asLong(payload.get("bindId"));
        if ("none".equals(bindType)) {
            bookId = null;
            bindId = null;
        } else if ("book".equals(bindType)) {
            bookId = bindId == null ? bookId : bindId;
            if (bookId != null) {
                requireBook(bookId);
            }
            bindId = bookId;
        } else if ("pathNode".equals(bindType) && bindId != null) {
            PathNode node = pathNodeMapper.selectOneById(bindId);
            if (node == null || Integer.valueOf(1).equals(node.getIsDeleted())) {
                throw new CustomerException("404", "路径节点不存在");
            }
            LearningPath path = node.getPathId() == null ? null : learningPathMapper.selectOneById(node.getPathId());
            if (bookId == null && path != null) {
                bookId = path.getBookId();
            }
        } else if (bookId != null) {
            requireBook(bookId);
        }
        resource.setUserId(userId);
        resource.setBookId(bookId);
        resource.setBindType(bindType);
        resource.setBindId(bindId);
        resource.setTitle(defaultString(text(payload.get("title")), defaultString(resource.getTitle(), "后台资源")));
        resource.setType(asInteger(payload.get("type"), resource.getType() == null ? 5 : resource.getType()));
        resource.setFileUrl(text(payload.get("fileUrl")));
        resource.setFileSize(asLong(payload.get("fileSize")) == null ? (resource.getFileSize() == null ? 0L : resource.getFileSize()) : asLong(payload.get("fileSize")));
        resource.setFileFormat(text(payload.get("fileFormat")));
        resource.setDownloadCount(asInteger(payload.get("downloadCount"), resource.getDownloadCount() == null ? 0 : resource.getDownloadCount()));
        resource.setDescription(text(payload.get("description")));
        resource.setVisibility(asInteger(payload.get("visibility"), resource.getVisibility() == null ? 1 : resource.getVisibility()));
        resource.setIsDeleted(visibleToIsDeleted(payload.get("visible"), resource.getIsDeleted() == null ? 0 : resource.getIsDeleted()));
        resource.setUpdateTime(now);
        if (creating) {
            resource.setCreateTime(now);
            resourceMapper.insert(resource);
        } else {
            resourceMapper.update(resource);
        }
        logOperation("resource", creating ? "create" : "update", resource.getId(), before, JSON.toJSONString(resource));
        return toResourceItem(resource);
    }

    @Transactional
    public Map<String, Object> changeResourceStatus(Map<String, Object> payload) {
        Long resourceId = asLong(payload.get("resourceId"));
        Integer visible = asInteger(payload.get("visible"), null);
        Resource resource = requireResource(resourceId);
        String before = JSON.toJSONString(resource);
        resource.setIsDeleted(Integer.valueOf(1).equals(visible) ? 0 : 1);
        resource.setUpdateTime(LocalDateTime.now());
        resourceMapper.update(resource);
        logOperation("resource", Integer.valueOf(1).equals(visible) ? "restore" : "hide", resourceId, before, JSON.toJSONString(resource));
        return toResourceItem(resource);
    }

    public Map<String, Object> listPaths(Integer pageNo, Integer pageSize, String keyword, Integer status, Integer coverImageStatus) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        if (status != null) {
            wrapper.and("status = ?", status);
        }
        if (coverImageStatus != null) {
            wrapper.and("cover_image_status = ?", coverImageStatus);
        }
        String normalizedKeyword = text(keyword);
        if (!normalizedKeyword.isBlank()) {
            String like = "%" + normalizedKeyword + "%";
            wrapper.and("(title LIKE ? OR description LIKE ?)", like, like);
        }
        long total = learningPathMapper.selectCountByQuery(wrapper);
        List<LearningPath> paths = learningPathMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(paths.stream().map(this::toPathItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> savePath(Map<String, Object> payload) {
        Long pathId = asLong(payload.get("id"));
        boolean creating = pathId == null;
        LearningPath path = creating ? new LearningPath() : requirePath(pathId);
        String before = creating ? null : JSON.toJSONString(path);
        LocalDateTime now = LocalDateTime.now();

        Long userId = asLong(payload.get("userId"));
        if (userId != null) {
            requireUser(userId);
        } else if (creating) {
            userId = createAdminManagedUser("后台路径用户");
        }
        Long bookId = asLong(payload.get("bookId"));
        if (bookId != null) {
            requireBook(bookId);
        }
        path.setUserId(userId);
        path.setBookId(bookId);
        path.setSourcePathId(asLong(payload.get("sourcePathId")));
        path.setTitle(defaultString(text(payload.get("title")), defaultString(path.getTitle(), "后台学习路径")));
        path.setDescription(text(payload.get("description")));
        path.setDifficulty(asInteger(payload.get("difficulty"), path.getDifficulty() == null ? 1 : path.getDifficulty()));
        path.setEstimatedHours(asInteger(payload.get("estimatedHours"), path.getEstimatedHours() == null ? 1 : path.getEstimatedHours()));
        path.setStatus(asInteger(payload.get("status"), path.getStatus() == null ? 0 : path.getStatus()));
        path.setCoverImage(text(payload.get("coverImage")));
        path.setCoverImageStatus(resolveAdminCoverImageStatus(payload, path));
        path.setViewCount(asInteger(payload.get("viewCount"), path.getViewCount() == null ? 0 : path.getViewCount()));
        path.setFavoriteCount(asInteger(payload.get("favoriteCount"), path.getFavoriteCount() == null ? 0 : path.getFavoriteCount()));
        path.setIsDeleted(asInteger(payload.get("isDeleted"), path.getIsDeleted() == null ? 0 : path.getIsDeleted()));
        path.setUpdateTime(now);
        if (creating) {
            path.setCreateTime(now);
            learningPathMapper.insert(path);
        } else {
            learningPathMapper.update(path);
        }
        logOperation("path", creating ? "create" : "update", path.getId(), before, JSON.toJSONString(path));
        return toPathItem(path);
    }

    @Transactional
    public Map<String, Object> changePathStatus(Map<String, Object> payload) {
        Long pathId = asLong(payload.get("pathId"));
        Integer status = asInteger(payload.get("status"), null);
        if (!List.of(0, 1, 2, 3).contains(status)) {
            throw new CustomerException("400", "学习路径状态不正确");
        }
        LearningPath path = requirePath(pathId);
        String before = JSON.toJSONString(path);
        path.setStatus(status);
        if (!text(path.getCoverImage()).isBlank()) {
            if (status == 1) {
                path.setCoverImageStatus(2);
            } else if (status == 2) {
                path.setCoverImageStatus(1);
            }
        }
        path.setUpdateTime(LocalDateTime.now());
        learningPathMapper.update(path);
        logOperation("path", "change_status", pathId, before, JSON.toJSONString(path));
        return toPathItem(path);
    }

    public Map<String, Object> listBanners(Integer pageNo, Integer pageSize, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        if (status != null) {
            wrapper.and("status = ?", status);
        }
        long total = bannerMapper.selectCountByQuery(wrapper);
        List<Banner> banners = bannerMapper.selectListByQuery(page(wrapper.orderBy("sort_order asc, id desc"), pageNo, pageSize));
        return pageResult(banners.stream().map(this::toBannerItem).toList(), total);
    }

    @Transactional
    public Map<String, Object> saveBanner(Map<String, Object> payload) {
        Long bannerId = asLong(payload.get("id"));
        Banner banner = bannerId == null ? new Banner() : bannerMapper.selectOneById(bannerId);
        if (banner == null) {
            throw new CustomerException("404", "Banner 不存在");
        }
        String before = bannerId == null ? null : JSON.toJSONString(banner);
        banner.setTitle(text(payload.get("title")));
        banner.setImageUrl(text(payload.get("imageUrl")));
        banner.setLink(text(payload.get("link")));
        banner.setSortOrder(asInteger(payload.get("sortOrder"), 0));
        banner.setStatus(asInteger(payload.get("status"), 1));
        banner.setUpdateTime(LocalDateTime.now());
        if (bannerId == null) {
            banner.setCreateTime(LocalDateTime.now());
            bannerMapper.insert(banner);
        } else {
            bannerMapper.update(banner);
        }
        logOperation("banner", bannerId == null ? "create" : "update", banner.getId(), before, JSON.toJSONString(banner));
        return toBannerItem(banner);
    }

    @Transactional
    public Map<String, Object> changeBannerStatus(Map<String, Object> payload) {
        Long bannerId = asLong(payload.get("bannerId"));
        Integer status = asInteger(payload.get("status"), null);
        if (!List.of(0, 1).contains(status)) {
            throw new CustomerException("400", "Banner 状态不正确");
        }
        Banner banner = bannerMapper.selectOneById(bannerId);
        if (banner == null) {
            throw new CustomerException("404", "Banner 不存在");
        }
        String before = JSON.toJSONString(banner);
        banner.setStatus(status);
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.update(banner);
        logOperation("banner", "change_status", bannerId, before, JSON.toJSONString(banner));
        return toBannerItem(banner);
    }

    public Map<String, Object> listLogs(Integer pageNo, Integer pageSize, String module) {
        QueryWrapper wrapper = QueryWrapper.create().where("1 = 1");
        String normalizedModule = text(module);
        if (!normalizedModule.isBlank()) {
            wrapper.and("module = ?", normalizedModule);
        }
        long total = adminOperationLogMapper.selectCountByQuery(wrapper);
        List<AdminOperationLog> logs = adminOperationLogMapper.selectListByQuery(page(wrapper.orderBy("id desc"), pageNo, pageSize));
        return pageResult(logs.stream().map(this::toLogItem).toList(), total);
    }

    private AdminUser currentAdmin() {
        Long adminId = AdminAuthContext.requireAdminId();
        AdminUser adminUser = adminUserMapper.selectOneByQuery(
            QueryWrapper.create().where("id = ?", adminId).and("is_deleted = 0").limit(1)
        );
        if (adminUser == null || !Integer.valueOf(1).equals(adminUser.getStatus())) {
            throw new CustomerException("401", "管理员账号不可用");
        }
        return adminUser;
    }

    private Map<String, Object> route(String name, String path, String component, String title, String icon) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("name", name);
        route.put("path", path);
        if (component != null) {
            route.put("component", component);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", title);
        meta.put("icon", icon);
        route.put("meta", meta);
        return route;
    }

    private QueryWrapper page(QueryWrapper wrapper, Integer pageNo, Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
        return wrapper.limit(safePageSize).offset((safePageNo - 1) * safePageSize);
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> items, long total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        return result;
    }

    private Map<String, Object> toUserItem(User user) {
        UserProfile profile = findProfile(user.getId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("openid", user.getOpenid());
        item.put("nickname", defaultString(user.getNickname(), "书友"));
        item.put("avatarUrl", defaultString(user.getAvatarUrl(), DEFAULT_AVATAR));
        item.put("mobile", defaultString(user.getMobile(), ""));
        item.put("gender", user.getGender());
        item.put("province", user.getProvince());
        item.put("city", user.getCity());
        item.put("lastLoginTime", user.getLastLoginTime());
        item.put("createTime", user.getCreateTime());
        item.put("updateTime", user.getUpdateTime());
        item.put("isDeleted", user.getIsDeleted());
        item.put("enabled", !Integer.valueOf(1).equals(user.getIsDeleted()));
        mergeProfile(item, profile);
        return item;
    }

    private Map<String, Object> toProfileItem(UserProfile profile) {
        Map<String, Object> item = new LinkedHashMap<>();
        User user = profile.getUserId() == null ? null : userMapper.selectOneById(profile.getUserId());
        item.put("id", profile.getId());
        item.put("userId", profile.getUserId());
        item.put("nickname", user == null ? "" : defaultString(user.getNickname(), "书友"));
        item.put("avatarUrl", user == null ? DEFAULT_AVATAR : defaultString(user.getAvatarUrl(), DEFAULT_AVATAR));
        item.put("mobile", user == null ? "" : defaultString(user.getMobile(), ""));
        mergeProfile(item, profile);
        return item;
    }

    private void mergeProfile(Map<String, Object> item, UserProfile profile) {
        item.put("studentId", profile == null ? "" : defaultString(profile.getStudentId(), ""));
        item.put("realName", profile == null ? "" : defaultString(profile.getRealName(), ""));
        item.put("school", profile == null ? "" : defaultString(profile.getSchool(), ""));
        item.put("department", profile == null ? "" : defaultString(profile.getDepartment(), ""));
        item.put("authStatus", profile == null || profile.getAuthStatus() == null ? 0 : profile.getAuthStatus());
        item.put("creditScore", profile == null || profile.getCreditScore() == null ? 88 : profile.getCreditScore());
        item.put("intro", profile == null ? "" : defaultString(profile.getIntro(), ""));
        item.put("studentCardImageUrl", profile == null ? "" : defaultString(profile.getStudentCardImageUrl(), ""));
        item.put("verifyType", profile == null ? "" : defaultString(profile.getVerifyType(), ""));
        item.put("verifySubmitTime", profile == null ? null : profile.getVerifySubmitTime());
        item.put("auditRemark", profile == null ? "" : defaultString(profile.getAuditRemark(), ""));
    }

    private Map<String, Object> toBookItem(Book book) {
        User seller = book.getUserId() == null ? null : userMapper.selectOneById(book.getUserId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", book.getId());
        item.put("userId", book.getUserId());
        item.put("sellerName", seller == null ? "" : defaultString(seller.getNickname(), "书友"));
        item.put("title", defaultString(book.getTitle(), ""));
        item.put("author", defaultString(book.getAuthor(), ""));
        item.put("publisher", defaultString(book.getPublisher(), ""));
        item.put("isbn", defaultString(book.getIsbn(), ""));
        item.put("category", defaultString(book.getCategory(), ""));
        item.put("price", book.getPrice());
        item.put("originalPrice", book.getOriginalPrice());
        item.put("condition", book.getCondition());
        List<String> imageList = normalizeImagePayload(book.getCoverImages());
        item.put("status", book.getStatus());
        item.put("cover", imageList.isEmpty() ? DEFAULT_AVATAR : imageList.get(0));
        item.put("coverImages", String.join(",", imageList));
        item.put("imageList", imageList);
        item.put("description", defaultString(book.getDescription(), ""));
        item.put("viewCount", book.getViewCount());
        item.put("favoriteCount", book.getFavoriteCount());
        item.put("annotationCount", book.getAnnotationCount());
        item.put("createTime", book.getCreateTime());
        item.put("updateTime", book.getUpdateTime());
        item.put("isDeleted", book.getIsDeleted());
        return item;
    }

    private Map<String, Object> toOrderItem(BookOrder order) {
        Book book = order.getBookId() == null ? null : bookMapper.selectOneById(order.getBookId());
        User buyer = order.getBuyerId() == null ? null : userMapper.selectOneById(order.getBuyerId());
        User seller = order.getSellerId() == null ? null : userMapper.selectOneById(order.getSellerId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", order.getId());
        item.put("orderNo", order.getOrderNo());
        item.put("bookId", order.getBookId());
        item.put("bookTitle", book == null ? "" : defaultString(book.getTitle(), ""));
        item.put("bookCover", book == null ? DEFAULT_AVATAR : firstImage(book.getCoverImages()));
        item.put("buyerId", order.getBuyerId());
        item.put("buyerName", buyer == null ? "" : defaultString(buyer.getNickname(), "书友"));
        item.put("sellerId", order.getSellerId());
        item.put("sellerName", seller == null ? "" : defaultString(seller.getNickname(), "书友"));
        item.put("totalAmount", order.getTotalAmount());
        item.put("status", order.getStatus());
        item.put("paymentMethod", order.getPaymentMethod());
        item.put("paymentTime", order.getPaymentTime());
        item.put("deliveryTime", order.getDeliveryTime());
        item.put("receiveTime", order.getReceiveTime());
        item.put("closeTime", order.getCloseTime());
        item.put("buyerMessage", defaultString(order.getBuyerMessage(), ""));
        item.put("receiverName", defaultString(order.getReceiverName(), ""));
        item.put("receiverPhone", defaultString(order.getReceiverPhone(), ""));
        item.put("receiverAddress", defaultString(order.getReceiverAddress(), ""));
        item.put("issueCount", orderIssueMapper.selectCountByQuery(
            QueryWrapper.create().where("order_id = ?", order.getId()).and("is_deleted = 0")
        ));
        item.put("createTime", order.getCreateTime());
        item.put("updateTime", order.getUpdateTime());
        return item;
    }

    private Map<String, Object> toOrderIssueItem(OrderIssue issue) {
        User user = issue.getUserId() == null ? null : userMapper.selectOneById(issue.getUserId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", issue.getId());
        item.put("orderId", issue.getOrderId());
        item.put("userId", issue.getUserId());
        item.put("userName", user == null ? "" : defaultString(user.getNickname(), "书友"));
        item.put("type", issue.getType());
        item.put("content", defaultString(issue.getContent(), ""));
        item.put("replyContent", defaultString(issue.getReplyContent(), ""));
        item.put("replyUserId", issue.getReplyUserId());
        item.put("status", issue.getStatus());
        item.put("replyTime", issue.getReplyTime());
        item.put("createTime", issue.getCreateTime());
        return item;
    }

    private Map<String, Object> toPostItem(Post post) {
        User author = post.getUserId() == null ? null : userMapper.selectOneById(post.getUserId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", post.getId());
        item.put("userId", post.getUserId());
        item.put("authorName", author == null ? "" : defaultString(author.getNickname(), "书友"));
        item.put("title", defaultString(post.getTitle(), ""));
        item.put("content", defaultString(post.getContent(), ""));
        item.put("type", post.getType());
        item.put("sharedPathId", post.getSharedPathId());
        item.put("viewCount", post.getViewCount());
        item.put("likeCount", post.getLikeCount());
        item.put("commentCount", post.getCommentCount());
        item.put("isDeleted", post.getIsDeleted());
        item.put("visible", !Integer.valueOf(1).equals(post.getIsDeleted()));
        item.put("createTime", post.getCreateTime());
        item.put("updateTime", post.getUpdateTime());
        return item;
    }

    private Map<String, Object> toCommentItem(Comment comment) {
        User author = comment.getUserId() == null ? null : userMapper.selectOneById(comment.getUserId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", comment.getId());
        item.put("postId", comment.getPostId());
        item.put("userId", comment.getUserId());
        item.put("authorName", author == null ? "" : defaultString(author.getNickname(), "书友"));
        item.put("content", defaultString(comment.getContent(), ""));
        item.put("likeCount", comment.getLikeCount());
        item.put("isDeleted", comment.getIsDeleted());
        item.put("visible", !Integer.valueOf(1).equals(comment.getIsDeleted()));
        item.put("createTime", comment.getCreateTime());
        return item;
    }

    private Map<String, Object> toReportItem(ContentReport report) {
        User user = report.getUserId() == null ? null : userMapper.selectOneById(report.getUserId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", report.getId());
        item.put("userId", report.getUserId());
        item.put("userName", user == null ? "" : defaultString(user.getNickname(), "书友"));
        item.put("targetType", report.getTargetType());
        item.put("targetId", report.getTargetId());
        item.put("targetTitle", resolveTargetTitle(report.getTargetType(), report.getTargetId()));
        item.put("reasonType", defaultString(report.getReasonType(), ""));
        item.put("content", defaultString(report.getContent(), ""));
        item.put("status", report.getStatus());
        item.put("createTime", report.getCreateTime());
        item.put("updateTime", report.getUpdateTime());
        return item;
    }

    private Map<String, Object> toFeedbackItem(UserFeedback feedback) {
        User user = feedback.getUserId() == null ? null : userMapper.selectOneById(feedback.getUserId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", feedback.getId());
        item.put("userId", feedback.getUserId());
        item.put("userName", user == null ? "" : defaultString(user.getNickname(), "书友"));
        item.put("feedbackType", defaultString(feedback.getFeedbackType(), ""));
        item.put("content", defaultString(feedback.getContent(), ""));
        item.put("contact", defaultString(feedback.getContact(), ""));
        item.put("pagePath", defaultString(feedback.getPagePath(), ""));
        item.put("status", feedback.getStatus());
        item.put("createTime", feedback.getCreateTime());
        item.put("updateTime", feedback.getUpdateTime());
        return item;
    }

    private Map<String, Object> toResourceItem(Resource resource) {
        User owner = resource.getUserId() == null ? null : userMapper.selectOneById(resource.getUserId());
        Book book = resource.getBookId() == null ? null : bookMapper.selectOneById(resource.getBookId());
        String bindType = defaultString(resource.getBindType(), "none");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", resource.getId());
        item.put("userId", resource.getUserId());
        item.put("ownerName", owner == null ? "" : defaultString(owner.getNickname(), "书友"));
        item.put("bookId", resource.getBookId());
        item.put("bookTitle", book == null ? "" : defaultString(book.getTitle(), ""));
        item.put("bindType", bindType);
        item.put("bindTypeLabel", mapResourceBindType(bindType));
        item.put("bindId", resource.getBindId());
        item.put("bindTargetTitle", resolveResourceBindTargetTitle(resource));
        item.put("title", defaultString(resource.getTitle(), ""));
        item.put("type", resource.getType());
        item.put("fileUrl", defaultString(resource.getFileUrl(), ""));
        item.put("fileSize", resource.getFileSize());
        item.put("fileFormat", defaultString(resource.getFileFormat(), ""));
        item.put("downloadCount", resource.getDownloadCount());
        item.put("description", defaultString(resource.getDescription(), ""));
        item.put("visibility", resource.getVisibility());
        item.put("isDeleted", resource.getIsDeleted());
        item.put("visible", !Integer.valueOf(1).equals(resource.getIsDeleted()));
        item.put("createTime", resource.getCreateTime());
        item.put("updateTime", resource.getUpdateTime());
        return item;
    }

    private String mapResourceBindType(String bindType) {
        return switch (defaultString(bindType, "none")) {
            case "book" -> "关联书籍";
            case "pathNode" -> "关联路径节点";
            default -> "未绑定";
        };
    }

    private String resolveResourceBindTargetTitle(Resource resource) {
        String bindType = defaultString(resource.getBindType(), "none");
        Long bindId = resource.getBindId();
        if ("book".equals(bindType)) {
            Long bookId = bindId == null ? resource.getBookId() : bindId;
            Book book = bookId == null ? null : bookMapper.selectOneById(bookId);
            return book == null ? "" : defaultString(book.getTitle(), "");
        }
        if ("pathNode".equals(bindType) && bindId != null) {
            PathNode node = pathNodeMapper.selectOneById(bindId);
            if (node == null || Integer.valueOf(1).equals(node.getIsDeleted())) {
                return "";
            }
            LearningPath path = node.getPathId() == null ? null : learningPathMapper.selectOneById(node.getPathId());
            String pathTitle = path == null ? "" : defaultString(path.getTitle(), "");
            String nodeTitle = defaultString(node.getTitle(), "路径节点");
            return pathTitle.isBlank() ? nodeTitle : pathTitle + " / " + nodeTitle;
        }
        return "";
    }

    private Map<String, Object> toPathItem(LearningPath path) {
        User author = path.getUserId() == null ? null : userMapper.selectOneById(path.getUserId());
        Book book = path.getBookId() == null ? null : bookMapper.selectOneById(path.getBookId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", path.getId());
        item.put("userId", path.getUserId());
        item.put("authorName", author == null ? "" : defaultString(author.getNickname(), "书友"));
        item.put("bookId", path.getBookId());
        item.put("bookTitle", book == null ? "" : defaultString(book.getTitle(), ""));
        item.put("sourcePathId", path.getSourcePathId());
        item.put("title", defaultString(path.getTitle(), ""));
        item.put("description", defaultString(path.getDescription(), ""));
        item.put("coverImage", defaultString(path.getCoverImage(), ""));
        item.put("coverImageStatus", path.getCoverImageStatus() == null ? 0 : path.getCoverImageStatus());
        item.put("coverImageStatusLabel", mapCoverImageStatus(path.getCoverImageStatus()));
        item.put("difficulty", path.getDifficulty());
        item.put("estimatedHours", path.getEstimatedHours());
        item.put("status", path.getStatus());
        item.put("viewCount", path.getViewCount());
        item.put("favoriteCount", path.getFavoriteCount());
        item.put("nodeCount", pathNodeMapper.selectCountByQuery(
            QueryWrapper.create().where("path_id = ?", path.getId()).and("is_deleted = 0")
        ));
        item.put("isDeleted", path.getIsDeleted());
        item.put("createTime", path.getCreateTime());
        item.put("updateTime", path.getUpdateTime());
        return item;
    }

    private Map<String, Object> toBannerItem(Banner banner) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", banner.getId());
        item.put("title", defaultString(banner.getTitle(), ""));
        item.put("imageUrl", defaultString(banner.getImageUrl(), ""));
        item.put("link", defaultString(banner.getLink(), ""));
        item.put("sortOrder", banner.getSortOrder());
        item.put("status", banner.getStatus());
        item.put("createTime", banner.getCreateTime());
        item.put("updateTime", banner.getUpdateTime());
        return item;
    }

    private Map<String, Object> toLogItem(AdminOperationLog log) {
        AdminUser admin = log.getAdminId() == null ? null : adminUserMapper.selectOneById(log.getAdminId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", log.getId());
        item.put("adminId", log.getAdminId());
        item.put("adminName", admin == null ? "" : defaultString(admin.getRealName(), admin.getUsername()));
        item.put("module", log.getModule());
        item.put("action", log.getAction());
        item.put("targetId", log.getTargetId());
        item.put("beforeData", log.getBeforeData());
        item.put("afterData", log.getAfterData());
        item.put("createTime", log.getCreateTime());
        return item;
    }

    private void hideReportedTarget(ContentReport report) {
        String targetType = text(report.getTargetType()).toLowerCase();
        Long targetId = report.getTargetId();
        if ("post".equals(targetType)) {
            Post post = postMapper.selectOneById(targetId);
            if (post != null) {
                post.setIsDeleted(1);
                post.setUpdateTime(LocalDateTime.now());
                postMapper.update(post);
            }
        } else if ("comment".equals(targetType)) {
            Comment comment = commentMapper.selectOneById(targetId);
            if (comment != null) {
                comment.setIsDeleted(1);
                commentMapper.update(comment);
            }
        } else if ("book".equals(targetType)) {
            Book book = bookMapper.selectOneById(targetId);
            if (book != null) {
                book.setStatus(4);
                book.setUpdateTime(LocalDateTime.now());
                bookMapper.update(book);
            }
        } else if ("resource".equals(targetType)) {
            Resource resource = resourceMapper.selectOneById(targetId);
            if (resource != null) {
                resource.setIsDeleted(1);
                resource.setUpdateTime(LocalDateTime.now());
                resourceMapper.update(resource);
            }
        } else if ("path".equals(targetType) || "learning_path".equals(targetType)) {
            LearningPath path = learningPathMapper.selectOneById(targetId);
            if (path != null) {
                path.setStatus(3);
                path.setUpdateTime(LocalDateTime.now());
                learningPathMapper.update(path);
            }
        }
    }

    private String resolveTargetTitle(String targetType, Long targetId) {
        if (targetId == null) {
            return "";
        }
        String type = text(targetType).toLowerCase();
        if ("post".equals(type)) {
            Post post = postMapper.selectOneById(targetId);
            return post == null ? "" : defaultString(post.getTitle(), "");
        }
        if ("comment".equals(type)) {
            Comment comment = commentMapper.selectOneById(targetId);
            return comment == null ? "" : ellipsis(comment.getContent(), 32);
        }
        if ("book".equals(type) || "order".equals(type)) {
            if ("book".equals(type)) {
                Book book = bookMapper.selectOneById(targetId);
                return book == null ? "" : defaultString(book.getTitle(), "");
            }
            BookOrder order = bookOrderMapper.selectOneById(targetId);
            return order == null ? "" : defaultString(order.getOrderNo(), "");
        }
        if ("resource".equals(type)) {
            Resource resource = resourceMapper.selectOneById(targetId);
            return resource == null ? "" : defaultString(resource.getTitle(), "");
        }
        if ("path".equals(type) || "learning_path".equals(type)) {
            LearningPath path = learningPathMapper.selectOneById(targetId);
            return path == null ? "" : defaultString(path.getTitle(), "");
        }
        return "";
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new CustomerException("400", "缺少用户 ID");
        }
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new CustomerException("404", "用户不存在");
        }
        return user;
    }

    private Book requireBook(Long bookId) {
        if (bookId == null) {
            throw new CustomerException("400", "缺少书籍 ID");
        }
        Book book = bookMapper.selectOneById(bookId);
        if (book == null) {
            throw new CustomerException("404", "书籍不存在");
        }
        return book;
    }

    private Post requirePost(Long postId) {
        if (postId == null) {
            throw new CustomerException("400", "缺少帖子 ID");
        }
        Post post = postMapper.selectOneById(postId);
        if (post == null) {
            throw new CustomerException("404", "帖子不存在");
        }
        return post;
    }

    private Comment requireComment(Long commentId) {
        if (commentId == null) {
            throw new CustomerException("400", "缺少评论 ID");
        }
        Comment comment = commentMapper.selectOneById(commentId);
        if (comment == null) {
            throw new CustomerException("404", "评论不存在");
        }
        return comment;
    }

    private ContentReport requireReport(Long reportId) {
        if (reportId == null) {
            throw new CustomerException("400", "缺少举报 ID");
        }
        ContentReport report = contentReportMapper.selectOneById(reportId);
        if (report == null) {
            throw new CustomerException("404", "举报不存在");
        }
        return report;
    }

    private UserFeedback requireFeedback(Long feedbackId) {
        if (feedbackId == null) {
            throw new CustomerException("400", "缺少反馈 ID");
        }
        UserFeedback feedback = userFeedbackMapper.selectOneById(feedbackId);
        if (feedback == null) {
            throw new CustomerException("404", "反馈不存在");
        }
        return feedback;
    }

    private Resource requireResource(Long resourceId) {
        if (resourceId == null) {
            throw new CustomerException("400", "缺少资源 ID");
        }
        Resource resource = resourceMapper.selectOneById(resourceId);
        if (resource == null) {
            throw new CustomerException("404", "资源不存在");
        }
        return resource;
    }

    private LearningPath requirePath(Long pathId) {
        if (pathId == null) {
            throw new CustomerException("400", "缺少学习路径 ID");
        }
        LearningPath path = learningPathMapper.selectOneById(pathId);
        if (path == null) {
            throw new CustomerException("404", "学习路径不存在");
        }
        return path;
    }

    private UserProfile getOrCreateProfile(Long userId) {
        requireUser(userId);
        UserProfile profile = findProfile(userId);
        if (profile != null) {
            return profile;
        }
        LocalDateTime now = LocalDateTime.now();
        profile = UserProfile.builder()
            .userId(userId)
            .authStatus(0)
            .creditScore(88)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        userProfileMapper.insert(profile);
        return profile;
    }

    private Long createAdminManagedUser(String nickname) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setOpenid("admin_" + UUID.randomUUID().toString().replace("-", ""));
        user.setNickname(defaultString(nickname, "后台用户"));
        user.setAvatarUrl(DEFAULT_AVATAR);
        user.setGender(0);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setIsDeleted(0);
        userMapper.insert(user);
        getOrCreateProfile(user.getId());
        return user.getId();
    }

    private void fillProfile(UserProfile profile, Map<String, Object> payload, LocalDateTime now) {
        profile.setStudentId(text(payload.get("studentId")));
        profile.setRealName(text(payload.get("realName")));
        profile.setSchool(text(payload.get("school")));
        profile.setDepartment(text(payload.get("department")));
        profile.setAuthStatus(asInteger(payload.get("authStatus"), profile.getAuthStatus() == null ? 0 : profile.getAuthStatus()));
        profile.setCreditScore(asInteger(payload.get("creditScore"), profile.getCreditScore() == null ? 88 : profile.getCreditScore()));
        profile.setIntro(text(payload.get("intro")));
        profile.setStudentCardImageUrl(text(payload.get("studentCardImageUrl")));
        profile.setVerifyType(text(payload.get("verifyType")));
        profile.setAuditRemark(text(payload.get("auditRemark")));
        if (Integer.valueOf(1).equals(profile.getAuthStatus()) && profile.getVerifySubmitTime() == null) {
            profile.setVerifySubmitTime(now);
        }
        if (profile.getCreateTime() == null) {
            profile.setCreateTime(now);
        }
        profile.setUpdateTime(now);
        profile.setIsDeleted(asInteger(payload.get("profileIsDeleted"), profile.getIsDeleted() == null ? 0 : profile.getIsDeleted()));
    }

    private UserProfile findProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileMapper.selectOneByQuery(
            QueryWrapper.create().where("user_id = ?", userId).and("is_deleted = 0").limit(1)
        );
    }

    private void logOperation(String module, String action, Long targetId, String beforeData, String afterData) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(AdminAuthContext.getAdminId() == null ? 0L : AdminAuthContext.getAdminId());
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        log.setCreateTime(LocalDateTime.now());
        adminOperationLogMapper.insert(log);
    }

    private String firstImage(String rawImages) {
        List<String> images = normalizeImagePayload(rawImages);
        return images.isEmpty() ? DEFAULT_AVATAR : images.get(0);
    }

    private List<String> normalizeImagePayload(Object rawImages) {
        Set<String> images = new LinkedHashSet<>();
        if (rawImages instanceof List<?> imageList) {
            imageList.stream()
                .map(this::text)
                .map(this::normalizeImageUrl)
                .filter(item -> !item.isBlank())
                .forEach(images::add);
        } else {
            String raw = text(rawImages);
            if (!raw.isBlank()) {
                Arrays.stream(raw.replace("[", "").replace("]", "").replace("\"", "").split(","))
                    .map(String::trim)
                    .map(this::normalizeImageUrl)
                    .filter(item -> !item.isBlank())
                    .forEach(images::add);
            }
        }
        return new ArrayList<>(images);
    }

    private String normalizeImageUrl(String rawUrl) {
        String url = text(rawUrl);
        if (url.isBlank()) {
            return "";
        }
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/")) {
            return url;
        }
        return normalizeBaseUrl(imageBaseUrl) + url;
    }

    private String resolveImageExtension(String originalFilename) {
        String fileName = defaultString(originalFilename, "");
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return ".jpg";
        }
        String extension = fileName.substring(dotIndex).toLowerCase();
        return List.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(extension) ? extension : ".jpg";
    }

    private void validateAdminImageFile(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new CustomerException("400", "请选择" + label);
        }
        if (file.getSize() > MAX_ADMIN_IMAGE_SIZE) {
            throw new CustomerException("400", label + "不能超过10MB");
        }
        String contentType = defaultString(file.getContentType(), "").toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new CustomerException("400", "仅支持上传图片文件");
        }
        resolveAllowedImageExtension(file.getOriginalFilename());
    }

    private String resolveAllowedImageExtension(String originalFilename) {
        String fileName = defaultString(originalFilename, "");
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new CustomerException("400", "图片文件缺少扩展名");
        }
        String extension = fileName.substring(dotIndex).toLowerCase();
        if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(extension)) {
            throw new CustomerException("400", "仅支持 JPG、PNG、GIF、WEBP 图片");
        }
        return extension;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = defaultString(baseUrl, "");
        if (normalized.isBlank()) {
            return "/";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String extractStudentCardFileName(String rawUrl) {
        String text = text(rawUrl);
        if (text.isBlank()) {
            return "";
        }
        int queryIndex = text.indexOf('?');
        if (queryIndex >= 0) {
            text = text.substring(0, queryIndex);
        }
        int fragmentIndex = text.indexOf('#');
        if (fragmentIndex >= 0) {
            text = text.substring(0, fragmentIndex);
        }
        text = text.replace('\\', '/');
        String fileName = text.substring(text.lastIndexOf('/') + 1);
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return "";
        }
        String lower = fileName.toLowerCase();
        if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp").stream().anyMatch(lower::endsWith)) {
            return "";
        }
        return fileName;
    }

    private Integer resolveAdminCoverImageStatus(Map<String, Object> payload, LearningPath path) {
        String coverImage = text(path.getCoverImage());
        if (coverImage.isBlank()) {
            return 0;
        }
        Integer currentStatus = path.getCoverImageStatus() == null ? 0 : path.getCoverImageStatus();
        Integer fallback = Integer.valueOf(1).equals(path.getStatus()) ? 2 : currentStatus;
        return asInteger(payload.get("coverImageStatus"), fallback);
    }

    private String mapCoverImageStatus(Integer status) {
        int value = status == null ? 0 : status;
        return switch (value) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已驳回";
            default -> "无需审核";
        };
    }

    private String placeholders(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(index -> "?").collect(Collectors.joining(","));
    }

    private String ellipsis(String value, int length) {
        String text = defaultString(value, "");
        return text.length() <= length ? text : text.substring(0, length) + "...";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer asInteger(Object value, Integer fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BigDecimal asBigDecimal(Object value, BigDecimal fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean flagEnabled(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = text(value).toLowerCase();
        if (List.of("1", "true", "yes", "on", "enabled").contains(text)) {
            return true;
        }
        if (List.of("0", "false", "no", "off", "disabled").contains(text)) {
            return false;
        }
        return fallback;
    }

    private Integer visibleToIsDeleted(Object visible, Integer fallbackIsDeleted) {
        if (visible == null || String.valueOf(visible).isBlank()) {
            return fallbackIsDeleted;
        }
        return flagEnabled(visible, !Integer.valueOf(1).equals(fallbackIsDeleted)) ? 0 : 1;
    }

    private String normalizeToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        String value = authorization.trim();
        if (value.startsWith("Bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }
}
