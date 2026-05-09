package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Annotation;
import com.book.bookflow.entity.AnnotationLike;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.User;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.AnnotationLikeMapper;
import com.book.bookflow.mapper.AnnotationMapper;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.service.AnnotationService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnnotationServiceImpl implements AnnotationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AnnotationMapper annotationMapper;
    private final AnnotationLikeMapper annotationLikeMapper;
    private final BookMapper bookMapper;
    private final BookOrderMapper bookOrderMapper;
    private final UserMapper userMapper;

    @Value("${app.image-base-url:}")
    private String imageBaseUrl;

    public AnnotationServiceImpl(AnnotationMapper annotationMapper,
                                 AnnotationLikeMapper annotationLikeMapper,
                                 BookMapper bookMapper,
                                 BookOrderMapper bookOrderMapper,
                                 UserMapper userMapper) {
        this.annotationMapper = annotationMapper;
        this.annotationLikeMapper = annotationLikeMapper;
        this.bookMapper = bookMapper;
        this.bookOrderMapper = bookOrderMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> getAnnotationList(Long bookId) {
        return getAnnotationList(bookId, false);
    }

    @Override
    public Map<String, Object> getAnnotationList(Long bookId, boolean mineOnly) {
        QueryWrapper wrapper = QueryWrapper.create()
            .where("book_id = ?", bookId)
            .and("is_deleted = 0");
        if (mineOnly) {
            wrapper.and("user_id = ?", AuthContext.requireUserId());
        }
        List<Annotation> annotations = annotationMapper.selectListByQuery(wrapper.orderBy("page_num asc, id asc"));
        Book book = bookId == null ? null : bookMapper.selectOneById(bookId);
        List<Map<String, Object>> items = annotations.stream().map(this::toAnnotationItem).collect(Collectors.toList());
        Long currentUserId = AuthContext.getUserId();
        List<AnnotationLike> likes = currentUserId == null || annotations.isEmpty()
            ? List.of()
            : annotationLikeMapper.selectListByQuery(
                QueryWrapper.create()
                    .where("user_id = ?", currentUserId)
                    .and("is_deleted = 0")
            );
        List<Map<String, Object>> pageNavItems = items.stream()
            .collect(Collectors.groupingBy(item -> (Integer) item.get("page"), LinkedHashMap::new, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> navItem = new LinkedHashMap<>();
                navItem.put("page", entry.getKey());
                navItem.put("count", entry.getValue());
                return navItem;
            })
            .collect(Collectors.toList());
        if (!likes.isEmpty()) {
            Map<Long, Boolean> likedMap = likes.stream().collect(Collectors.toMap(AnnotationLike::getAnnotationId, item -> true, (left, right) -> left));
            for (Map<String, Object> item : items) {
                Object id = item.get("id");
                Long annotationId = id instanceof Number ? ((Number) id).longValue() : asLong(id);
                item.put("liked", annotationId != null && likedMap.containsKey(annotationId));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookId", bookId);
        result.put("mineOnly", mineOnly);
        result.put("bookTitle", book != null ? defaultString(book.getTitle(), "书籍批注") : "书籍批注");
        result.put("total", items.size());
        result.put("canAdd", canCurrentUserAdd(book));
        result.put("pageNavItems", pageNavItems);
        result.put("annotations", items);
        return result;
    }

    @Override
    public Map<String, Object> createAnnotation(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long bookId = asLong(payload.get("bookId"));
        Book book = bookId == null ? null : bookMapper.selectOneById(bookId);
        if (book == null || isDeleted(book.getIsDeleted())) {
            throw new CustomerException("404", "书籍不存在");
        }
        if (!canCurrentUserAdd(book)) {
            throw new CustomerException("403", "只有书主或已购买用户可以发布批注");
        }

        LocalDateTime now = LocalDateTime.now();
        Annotation annotation = Annotation.builder()
            .bookId(bookId)
            .userId(currentUserId)
            .pageNum(asInteger(payload.get("page"), 1))
            .content(defaultString(payload.get("content"), ""))
            .positionText(defaultString(payload.get("positionText"), ""))
            .imageUrl(defaultString(payload.get("imageUrl"), ""))
            .type(mapType(defaultString(payload.get("type"), "highlight")))
            .visibility(asInteger(payload.get("visibility"), 1))
            .likeCount(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        annotationMapper.insert(annotation);

        int count = book.getAnnotationCount() == null ? 0 : book.getAnnotationCount();
        book.setAnnotationCount(count + 1);
        book.setUpdateTime(now);
        bookMapper.update(book);
        return toAnnotationItem(annotation);
    }

    @Override
    public Map<String, Object> uploadAnnotationImage(MultipartFile file) {
        Long currentUserId = AuthContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new CustomerException("400", "请选择批注图片");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String fileName = currentUserId + "_" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDirectory = Paths.get("uploads", "annotation");
        Path targetFile = targetDirectory.resolve(fileName);
        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CustomerException("500", "批注图片上传失败");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", normalizeBaseUrl(imageBaseUrl) + "uploads/annotation/" + fileName);
        result.put("fileName", fileName);
        return result;
    }

    @Override
    public Map<String, Object> toggleLike(Long annotationId) {
        Long currentUserId = AuthContext.requireUserId();
        if (annotationId == null) {
            throw new CustomerException("400", "批注参数错误");
        }
        Annotation annotation = annotationMapper.selectOneById(annotationId);
        if (annotation == null || isDeleted(annotation.getIsDeleted())) {
            throw new CustomerException("404", "批注不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        AnnotationLike like = annotationLikeMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("annotation_id = ?", annotationId)
                .and("user_id = ?", currentUserId)
                .limit(1)
        );
        boolean liked;
        int currentCount = annotation.getLikeCount() == null ? 0 : annotation.getLikeCount();
        if (like == null) {
            like = AnnotationLike.builder()
                .annotationId(annotationId)
                .userId(currentUserId)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            annotationLikeMapper.insert(like);
            liked = true;
            annotation.setLikeCount(currentCount + 1);
        } else if (isDeleted(like.getIsDeleted())) {
            like.setIsDeleted(0);
            like.setUpdateTime(now);
            annotationLikeMapper.update(like);
            liked = true;
            annotation.setLikeCount(currentCount + 1);
        } else {
            like.setIsDeleted(1);
            like.setUpdateTime(now);
            annotationLikeMapper.update(like);
            liked = false;
            annotation.setLikeCount(Math.max(0, currentCount - 1));
        }
        annotation.setUpdateTime(now);
        annotationMapper.update(annotation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("annotationId", annotationId);
        result.put("liked", liked);
        result.put("likeCount", annotation.getLikeCount() == null ? 0 : annotation.getLikeCount());
        return result;
    }

    private Map<String, Object> toAnnotationItem(Annotation annotation) {
        User user = annotation.getUserId() == null ? null : userMapper.selectOneById(annotation.getUserId());
        String typeKey = switch (annotation.getType() == null ? 1 : annotation.getType()) {
            case 2 -> "question";
            case 3 -> "insight";
            default -> "highlight";
        };
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", annotation.getId());
        item.put("page", annotation.getPageNum() == null ? 1 : annotation.getPageNum());
        item.put("type", typeKey);
        item.put("content", defaultString(annotation.getContent(), ""));
        item.put("positionText", defaultString(annotation.getPositionText(), ""));
        item.put("imageUrl", defaultString(annotation.getImageUrl(), ""));
        item.put("nickname", user != null && user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : "校园书友");
        item.put("anonymous", false);
        item.put("createdAt", annotation.getCreateTime() == null ? "" : TIME_FORMATTER.format(annotation.getCreateTime()));
        item.put("likeCount", annotation.getLikeCount() == null ? 0 : annotation.getLikeCount());
        item.put("commentCount", 0);
        item.put("liked", false);
        return item;
    }

    private boolean canCurrentUserAdd(Book book) {
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId == null || book == null || isDeleted(book.getIsDeleted())) {
            return false;
        }
        if (currentUserId.equals(book.getUserId())) {
            return true;
        }
        List<BookOrder> orders = bookOrderMapper.selectListByQuery(
            QueryWrapper.create()
                .where("book_id = ?", book.getId())
                .and("buyer_id = ?", currentUserId)
                .and("is_deleted = 0")
        );
        return orders.stream()
            .map(BookOrder::getStatus)
            .anyMatch(status -> status != null && (status == 2 || status == 3 || status == 4 || status == 6));
    }

    private int mapType(String type) {
        return switch (type) {
            case "question" -> 2;
            case "insight" -> 3;
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

    private Integer asInteger(Object value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
