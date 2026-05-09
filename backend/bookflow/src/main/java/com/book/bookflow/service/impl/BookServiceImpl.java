package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.LearningPath;
import com.book.bookflow.entity.Resource;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserFavorite;
import com.book.bookflow.entity.UserProfile;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.AnnotationMapper;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.LearningPathMapper;
import com.book.bookflow.mapper.ResourceMapper;
import com.book.bookflow.mapper.UserFavoriteMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.mapper.UserProfileMapper;
import com.book.bookflow.service.BookService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private static final String DEFAULT_AVATAR = "/static/logo.png";
    private static final String DEFAULT_NICKNAME = "书友";

    private final BookMapper bookMapper;
    private final AnnotationMapper annotationMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final ResourceMapper resourceMapper;
    private final LearningPathMapper learningPathMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final BookOrderMapper bookOrderMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.image-base-url:}")
    private String imageBaseUrl;

    public BookServiceImpl(BookMapper bookMapper,
                           AnnotationMapper annotationMapper,
                           UserMapper userMapper,
                           UserProfileMapper userProfileMapper,
                           ResourceMapper resourceMapper,
                           LearningPathMapper learningPathMapper,
                           UserFavoriteMapper userFavoriteMapper,
                           BookOrderMapper bookOrderMapper,
                           JdbcTemplate jdbcTemplate) {
        this.bookMapper = bookMapper;
        this.annotationMapper = annotationMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.resourceMapper = resourceMapper;
        this.learningPathMapper = learningPathMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.bookOrderMapper = bookOrderMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> getBooksList() {
        // 获取书籍列表
        List<Book> books = bookMapper.selectListByQuery(
            QueryWrapper.create()
                .where("is_deleted = 0")
                .and("status = 1")
                .orderBy("id desc")
                .limit(40)
        );
        return books.stream().map(this::toPublicBookMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Map<String, Object>>> getBooksByCategory() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        List<String> categories = jdbcTemplate.queryForList(
            """
                SELECT category
                FROM `book`
                WHERE is_deleted = 0
                  AND status = 1
                  AND category IS NOT NULL
                  AND category <> ''
                GROUP BY category
                ORDER BY MAX(id) DESC
                LIMIT 12
                """,
            String.class
        );
        for (String category : categories) {
            List<Book> books = bookMapper.selectListByQuery(
                QueryWrapper.create()
                    .where("is_deleted = 0")
                    .and("status = 1")
                    .and("category = ?", category)
                    .orderBy("id desc")
                    .limit(10)
            );
            result.put(category, books.stream().map(this::toPublicBookMap).collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public Map<String, Object> searchBooks(String keyword, String category, Integer pageNo, Integer pageSize) {
        // 书籍搜索
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 30));
        String normalizedKeyword = defaultIfBlank(keyword, "").trim();
        String normalizedCategory = defaultIfBlank(category, "").trim();

        QueryWrapper wrapper = QueryWrapper.create()
            .where("is_deleted = 0")
            .and("status = 1");
        if (!normalizedCategory.isBlank()) {
            wrapper.and("category = ?", normalizedCategory);
        }
        if (!normalizedKeyword.isBlank()) {
            String likeValue = "%" + normalizedKeyword + "%";
            wrapper.and("(title like ? or author like ? or publisher like ? or isbn like ?)",
                likeValue, likeValue, likeValue, likeValue);
        }

        long total = bookMapper.selectCountByQuery(wrapper);
        List<Book> books = bookMapper.selectListByQuery(
            wrapper.orderBy("id desc")
                .limit(safePageSize)
                .offset((safePageNo - 1) * safePageSize)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", books.stream().map(this::toPublicBookMap).collect(Collectors.toList()));
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        result.put("total", total);
        result.put("hasMore", (long) safePageNo * safePageSize < total);
        result.put("keyword", normalizedKeyword);
        result.put("category", normalizedCategory);
        return result;
    }

    @Override
    public Map<String, Object> getBookDetail(Long bookId) {
        // 获取书籍详情
        Book book = requireBook(bookId);
        Long currentUserId = AuthContext.getUserId();
        boolean owner = currentUserId != null && currentUserId.equals(book.getUserId());
        if (!owner && !Objects.equals(book.getStatus(), 1) && !hasBuyerAccess(currentUserId, book.getId())) {
            throw new CustomerException("403", "当前书籍不可查看");
        }
        return toDetailBookMap(book, currentUserId);
    }

    @Override
    public Map<String, Object> publishBook(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        LocalDateTime now = LocalDateTime.now();
        int status = normalizeBookStatus(asInteger(payload.get("status"), 1));
        Book book = Book.builder()
            .userId(currentUserId)
            .isbn(asString(payload.get("isbn")))
            .title(defaultIfBlank(asString(payload.get("title")), "未命名书籍"))
            .author(defaultIfBlank(asString(payload.get("author")), "未知作者"))
            .publisher(defaultIfBlank(asString(payload.get("publisher")), ""))
            .coverImages(joinImages(payload.get("images")))
            .description(asString(payload.get("description")))
            .category(defaultIfBlank(asString(payload.get("category")), "其他"))
            .price(asBigDecimal(payload.get("price"), BigDecimal.ZERO))
            .condition(asInteger(payload.get("condition"), 3))
            .status(status)
            .viewCount(0)
            .favoriteCount(0)
            .annotationCount(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        bookMapper.insert(book);
        return toDetailBookMap(book, currentUserId);
    }

    @Override
    public Map<String, Object> uploadBookImage(MultipartFile file) {
        Long currentUserId = AuthContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new CustomerException("400", "请选择书籍图片");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        String fileName = currentUserId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
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

    @Override
    public Map<String, Object> updateBook(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Book book = requireOwnBook(asLong(payload.get("bookId")));
        book.setTitle(defaultIfBlank(asString(payload.get("title")), book.getTitle()));
        book.setAuthor(defaultIfBlank(asString(payload.get("author")), book.getAuthor()));
        book.setPublisher(defaultIfBlank(asString(payload.get("publisher")), book.getPublisher()));
        book.setIsbn(defaultIfBlank(asString(payload.get("isbn")), book.getIsbn()));
        book.setCategory(defaultIfBlank(asString(payload.get("category")), book.getCategory()));
        book.setDescription(defaultIfBlank(asString(payload.get("description")), book.getDescription()));
        book.setCondition(asInteger(payload.get("condition"), book.getCondition() == null ? 3 : book.getCondition()));
        book.setPrice(asBigDecimal(payload.get("price"), book.getPrice() == null ? BigDecimal.ZERO : book.getPrice()));
        String images = joinImages(payload.get("images"));
        if (!images.isBlank()) {
            book.setCoverImages(images);
        }
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.update(book);
        return toDetailBookMap(book, currentUserId);
    }

    @Override
    public Map<String, Object> changeBookStatus(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Book book = requireOwnBook(asLong(payload.get("bookId")));
        int status = normalizeBookStatus(asInteger(payload.get("status"), book.getStatus() == null ? 1 : book.getStatus()));
        book.setStatus(status);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.update(book);
        return toDetailBookMap(book, currentUserId);
    }

    private Map<String, Object> toPublicBookMap(Book book) {
        User seller = book.getUserId() == null ? null : userMapper.selectOneById(book.getUserId());
        List<String> images = normalizeImages(book.getCoverImages());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", book.getId());
        map.put("sellerId", book.getUserId());
        map.put("sellerName", seller != null ? defaultIfBlank(seller.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        map.put("sellerAvatar", seller != null ? defaultIfBlank(seller.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR);
        map.put("sellerScore", 4.4D);
        map.put("title", defaultIfBlank(book.getTitle(), "\u672a\u547d\u540d\u4e66\u7c4d"));
        map.put("author", defaultIfBlank(book.getAuthor(), "\u672a\u77e5\u4f5c\u8005"));
        map.put("publisher", defaultIfBlank(book.getPublisher(), ""));
        map.put("isbn", defaultIfBlank(book.getIsbn(), ""));
        map.put("price", book.getPrice() == null ? BigDecimal.ZERO : book.getPrice());
        map.put("annotations", book.getAnnotationCount() == null ? 0 : book.getAnnotationCount());
        map.put("annotationCount", book.getAnnotationCount() == null ? 0 : book.getAnnotationCount());
        map.put("category", defaultIfBlank(book.getCategory(), "\u5176\u4ed6"));
        map.put("categoryName", defaultIfBlank(book.getCategory(), "\u5176\u4ed6"));
        map.put("condition", book.getCondition() == null ? 3 : book.getCondition());
        map.put("conditionLabel", toConditionLabel(book.getCondition()));
        map.put("status", normalizeBookStatus(book.getStatus() == null ? 1 : book.getStatus()));
        map.put("statusLabel", mapBookStatus(book.getStatus()));
        map.put("isSold", Objects.equals(book.getStatus(), 3));
        map.put("description", defaultIfBlank(book.getDescription(), ""));
        map.put("resourceCount", 0);
        map.put("cover", images.isEmpty() ? DEFAULT_AVATAR : images.get(0));
        map.put("images", String.join(",", images));
        map.put("coverImages", String.join(",", images));
        return map;
    }

    private Map<String, Object> toDetailBookMap(Book book, Long currentUserId) {
        User seller = book.getUserId() == null ? null : userMapper.selectOneById(book.getUserId());
        UserProfile profile = book.getUserId() == null ? null : userProfileMapper.selectOneByQuery(
            QueryWrapper.create().where("user_id = ?", book.getUserId()).and("is_deleted = 0").limit(1)
        );
        List<String> images = normalizeImages(book.getCoverImages());
        long annotationCount = annotationMapper.selectCountByQuery(
            QueryWrapper.create().where("book_id = ?", book.getId()).and("is_deleted = 0")
        );
        long resourceCount = resourceMapper.selectCountByQuery(
            QueryWrapper.create()
                .where("is_deleted = 0")
                .and("((bind_type = 'book' and bind_id = ?) or (book_id = ? and (bind_type is null or bind_type = '' or bind_type = 'none')))",
                    book.getId(), book.getId())
        );
        List<LearningPath> paths = learningPathMapper.selectListByQuery(
            QueryWrapper.create()
                .where("(book_id = ? or source_path_id = ?)", book.getId(), book.getId())
                .and("is_deleted = 0")
                .orderBy("id desc")
                .limit(3)
        );

        boolean owner = currentUserId != null && currentUserId.equals(book.getUserId());
        boolean buyerAccess = hasBuyerAccess(currentUserId, book.getId());
        boolean favorited = currentUserId != null && userFavoriteMapper.selectCountByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("target_type = 1")
                .and("target_id = ?", book.getId())
        ) > 0;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", book.getId());
        map.put("sellerId", book.getUserId());
        map.put("sellerName", seller != null ? defaultIfBlank(seller.getNickname(), DEFAULT_NICKNAME) : DEFAULT_NICKNAME);
        map.put("sellerAvatar", seller != null ? defaultIfBlank(seller.getAvatarUrl(), DEFAULT_AVATAR) : DEFAULT_AVATAR);
        map.put("sellerScore", profile != null && profile.getCreditScore() != null ? profile.getCreditScore() / 20.0 : 4.4D);
        map.put("title", defaultIfBlank(book.getTitle(), "未命名书籍"));
        map.put("author", defaultIfBlank(book.getAuthor(), "未知作者"));
        map.put("publisher", defaultIfBlank(book.getPublisher(), ""));
        map.put("isbn", defaultIfBlank(book.getIsbn(), ""));
        map.put("price", book.getPrice() == null ? BigDecimal.ZERO : book.getPrice());
        map.put("annotations", annotationCount);
        map.put("annotationCount", annotationCount);
        map.put("category", defaultIfBlank(book.getCategory(), "其他"));
        map.put("categoryName", defaultIfBlank(book.getCategory(), "其他"));
        map.put("condition", book.getCondition() == null ? 3 : book.getCondition());
        map.put("conditionLabel", toConditionLabel(book.getCondition()));
        map.put("status", normalizeBookStatus(book.getStatus() == null ? 1 : book.getStatus()));
        map.put("statusLabel", mapBookStatus(book.getStatus()));
        map.put("isSold", Objects.equals(book.getStatus(), 3));
        map.put("description", defaultIfBlank(book.getDescription(), ""));
        map.put("remark", owner ? "可在书架中切换在售/私有/下架状态" : "支持校内面交");
        map.put("resourceCount", resourceCount);
        map.put("cover", images.isEmpty() ? DEFAULT_AVATAR : images.get(0));
        map.put("images", String.join(",", images));
        map.put("coverImages", String.join(",", images));
        map.put("favorited", favorited);
        map.put("learningPaths", paths.stream().map(this::toPathCard).collect(Collectors.toList()));
        map.put("canBuy", currentUserId != null && !owner && Objects.equals(book.getStatus(), 1));
        map.put("canAnnotate", owner || buyerAccess);
        map.put("canManage", owner);
        map.put("canEdit", owner);
        map.put("canUploadResource", owner);
        map.put("canContact", currentUserId != null && !owner);
        map.put("owner", owner);
        map.put("buyerAccess", buyerAccess);
        return map;
    }

    private Map<String, Object> toPathCard(LearningPath path) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", path.getId());
        item.put("name", defaultIfBlank(path.getTitle(), "学习路径"));
        item.put("intro", defaultIfBlank(path.getDescription(), ""));
        item.put("difficulty", mapPathDifficulty(path.getDifficulty()));
        item.put("duration", formatHours(path.getEstimatedHours()));
        item.put("status", path.getStatus());
        item.put("learners", learningPathMapper.selectCountByQuery(
            QueryWrapper.create().where("source_path_id = ?", path.getId()).and("is_deleted = 0")
        ));
        return item;
    }

    private boolean hasBuyerAccess(Long currentUserId, Long bookId) {
        if (currentUserId == null || bookId == null) {
            return false;
        }
        List<BookOrder> orders = bookOrderMapper.selectListByQuery(
            QueryWrapper.create()
                .where("book_id = ?", bookId)
                .and("buyer_id = ?", currentUserId)
                .and("is_deleted = 0")
        );
        return orders.stream()
            .map(BookOrder::getStatus)
            .anyMatch(status -> status != null && status >= 2 && status != 5);
    }

    private Book requireBook(Long bookId) {
        if (bookId == null) {
            throw new CustomerException("400", "书籍参数错误");
        }
        Book book = bookMapper.selectOneById(bookId);
        if (book == null || Objects.equals(book.getIsDeleted(), 1)) {
            throw new CustomerException("404", "书籍不存在");
        }
        return book;
    }

    private Book requireOwnBook(Long bookId) {
        Long currentUserId = AuthContext.requireUserId();
        Book book = requireBook(bookId);
        if (!currentUserId.equals(book.getUserId())) {
            throw new CustomerException("403", "无权操作该书籍");
        }
        return book;
    }

    private List<String> normalizeImages(String rawImages) {
        if (rawImages == null || rawImages.isBlank()) {
            return new ArrayList<>(Collections.singletonList(DEFAULT_AVATAR));
        }
        Set<String> images = new LinkedHashSet<>();
        Arrays.stream(rawImages.replace("[", "").replace("]", "").replace("\"", "").split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .map(this::toImageUrl)
            .forEach(images::add);
        if (images.isEmpty()) {
            images.add(DEFAULT_AVATAR);
        }
        return new ArrayList<>(images);
    }

    private String joinImages(Object rawImages) {
        if (rawImages instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(item -> item != null && !item.isBlank()).collect(Collectors.joining(","));
        }
        return asString(rawImages);
    }

    private String toImageUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_AVATAR;
        }
        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("/")) {
            return raw;
        }
        return normalizeBaseUrl(imageBaseUrl) + raw;
    }

    private String toConditionLabel(Integer condition) {
        int value = condition == null ? 3 : condition;
        return switch (value) {
            case 1 -> "全新";
            case 2 -> "9成新";
            case 3 -> "8成新";
            case 4 -> "7成新";
            default -> "6成新及以下";
        };
    }

    private String mapBookStatus(Integer status) {
        int value = normalizeBookStatus(status == null ? 1 : status);
        return switch (value) {
            case 0 -> "私有自藏";
            case 2 -> "锁定交易中";
            case 3 -> "已售";
            case 4 -> "已下架";
            default -> "在售";
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

    private String formatHours(Integer hours) {
        int value = hours == null ? 0 : hours;
        return value <= 0 ? "时长待补充" : value + "小时";
    }

    private int normalizeBookStatus(int status) {
        return switch (status) {
            case 0, 1, 2, 3, 4 -> status;
            default -> 1;
        };
    }

    private String resolveExtension(String originalFilename) {
        String fileName = originalFilename == null ? "" : originalFilename.trim();
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
        String text = baseUrl == null || baseUrl.isBlank() ? "/" : baseUrl;
        return text.endsWith("/") ? text : text + "/";
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
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

    private BigDecimal asBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
