package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.LearningPath;
import com.book.bookflow.entity.PathNode;
import com.book.bookflow.entity.Resource;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.BookOrderMapper;
import com.book.bookflow.mapper.LearningPathMapper;
import com.book.bookflow.mapper.PathNodeMapper;
import com.book.bookflow.mapper.ResourceMapper;
import com.book.bookflow.service.ResourceService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResourceServiceImpl implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final BookMapper bookMapper;
    private final BookOrderMapper bookOrderMapper;
    private final PathNodeMapper pathNodeMapper;
    private final LearningPathMapper learningPathMapper;

    @Value("${app.image-base-url:}")
    private String imageBaseUrl;

    public ResourceServiceImpl(ResourceMapper resourceMapper,
                               BookMapper bookMapper,
                               BookOrderMapper bookOrderMapper,
                               PathNodeMapper pathNodeMapper,
                               LearningPathMapper learningPathMapper) {
        this.resourceMapper = resourceMapper;
        this.bookMapper = bookMapper;
        this.bookOrderMapper = bookOrderMapper;
        this.pathNodeMapper = pathNodeMapper;
        this.learningPathMapper = learningPathMapper;
    }

    @Override
    public List<Map<String, Object>> getMyResources() {
        Long currentUserId = AuthContext.requireUserId();
        List<Resource> resources = resourceMapper.selectListByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("is_deleted = 0").orderBy("id desc").limit(100)
        );
        return resources.stream().map(this::toResourceItem).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getResourceDetail(Long id) {
        if (id == null) {
            throw new CustomerException("400", "资源参数错误");
        }
        Resource resource = resourceMapper.selectOneById(id);
        if (resource == null || isDeleted(resource.getIsDeleted())) {
            throw new CustomerException("404", "资源不存在");
        }
        Long currentUserId = AuthContext.getUserId();
        if (!canViewResource(currentUserId, resource, resource.getBookId())) {
            throw new CustomerException("403", "无权查看该资源");
        }
        return toResourceItem(resource);
    }

    @Override
    public List<Map<String, Object>> getResources(Long bookId, Long pathNodeId) {
        Long currentUserId = AuthContext.getUserId();
        Set<Long> resourceIds = new LinkedHashSet<>();
        if (bookId != null) {
            List<Resource> bookResources = resourceMapper.selectListByQuery(
                QueryWrapper.create()
                    .where("is_deleted = 0")
                    .and("((bind_type = 'book' and bind_id = ?) or (book_id = ? and (bind_type is null or bind_type = '' or bind_type = 'none')))",
                        bookId, bookId)
                    .orderBy("id desc")
            );
            bookResources.stream().map(Resource::getId).filter(Objects::nonNull).forEach(resourceIds::add);
        }
        if (pathNodeId != null) {
            PathNode node = pathNodeMapper.selectOneById(pathNodeId);
            if (node != null && !isDeleted(node.getIsDeleted())) {
                splitIds(node.getResourceIds()).forEach(resourceIds::add);
            }
            List<Resource> pathNodeResources = resourceMapper.selectListByQuery(
                QueryWrapper.create()
                    .where("is_deleted = 0")
                    .and("bind_type = 'pathNode'")
                    .and("bind_id = ?", pathNodeId)
                    .orderBy("id desc")
            );
            pathNodeResources.stream().map(Resource::getId).filter(Objects::nonNull).forEach(resourceIds::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long resourceId : resourceIds) {
            Resource resource = resourceMapper.selectOneById(resourceId);
            if (resource == null || isDeleted(resource.getIsDeleted())) {
                continue;
            }
            if (!canViewResource(currentUserId, resource, bookId)) {
                continue;
            }
            result.add(toResourceItem(resource));
        }
        return result;
    }

    @Override
    public Map<String, Object> uploadResourceFile(MultipartFile file) {
        Long currentUserId = AuthContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new CustomerException("400", "请选择资源文件");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        String fileName = currentUserId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get("uploads", "resource");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CustomerException("500", "资源上传失败");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", normalizeBaseUrl(imageBaseUrl) + "uploads/resource/" + fileName);
        result.put("fileName", fileName);
        return result;
    }

    @Override
    public Map<String, Object> createResource(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        LocalDateTime now = LocalDateTime.now();
        Resource resource = Resource.builder()
            .userId(currentUserId)
            .bookId(resolveBookId(payload))
            .bindType(resolveBindType(payload))
            .bindId(asLong(payload.get("bindId")))
            .title(defaultString(payload.get("title"), "未命名资源"))
            .type(asInteger(payload.get("type"), 5))
            .fileUrl(defaultString(payload.get("fileUrl"), ""))
            .fileSize(asLong(payload.get("fileSize")))
            .fileFormat(defaultString(payload.get("fileFormat"), ""))
            .downloadCount(0)
            .description(defaultString(payload.get("description"), ""))
            .visibility(normalizeVisibility(asInteger(payload.get("visibility"), 1)))
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        validateResourceOwnership(resource, currentUserId);
        resourceMapper.insert(resource);
        syncPathNodeBinding(resource, null);
        return toResourceItem(resource);
    }

    @Override
    public Map<String, Object> updateResource(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long id = asLong(payload.get("id"));
        if (id == null) {
            throw new CustomerException("400", "资源参数错误");
        }
        Resource resource = resourceMapper.selectOneById(id);
        if (resource == null || isDeleted(resource.getIsDeleted()) || !currentUserId.equals(resource.getUserId())) {
            throw new CustomerException("404", "资源不存在");
        }
        String previousBindType = resource.getBindType();
        Long previousBindId = resource.getBindId();
        resource.setBookId(resolveBookId(payload));
        resource.setBindType(resolveBindType(payload));
        resource.setBindId(asLong(payload.get("bindId")));
        resource.setTitle(defaultString(payload.get("title"), resource.getTitle()));
        resource.setType(asInteger(payload.get("type"), resource.getType() == null ? 5 : resource.getType()));
        resource.setFileUrl(defaultString(payload.get("fileUrl"), resource.getFileUrl()));
        resource.setFileSize(asLong(payload.get("fileSize")) == null ? resource.getFileSize() : asLong(payload.get("fileSize")));
        resource.setFileFormat(defaultString(payload.get("fileFormat"), resource.getFileFormat()));
        resource.setDescription(defaultString(payload.get("description"), resource.getDescription()));
        resource.setVisibility(normalizeVisibility(asInteger(payload.get("visibility"), resource.getVisibility() == null ? 1 : resource.getVisibility())));
        resource.setUpdateTime(LocalDateTime.now());
        validateResourceOwnership(resource, currentUserId);
        resourceMapper.update(resource);
        syncPathNodeBinding(resource, previousBindType == null ? null : bindingSnapshot(previousBindType, previousBindId));
        return toResourceItem(resource);
    }

    @Override
    public Map<String, Object> deleteResource(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long id = asLong(payload.get("id"));
        if (id == null) {
            throw new CustomerException("400", "资源参数错误");
        }
        Resource resource = resourceMapper.selectOneById(id);
        if (resource == null || isDeleted(resource.getIsDeleted()) || !currentUserId.equals(resource.getUserId())) {
            throw new CustomerException("404", "资源不存在");
        }
        resource.setIsDeleted(1);
        resource.setUpdateTime(LocalDateTime.now());
        resourceMapper.update(resource);
        syncPathNodeBinding(resource, bindingSnapshot(resource.getBindType(), resource.getBindId()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("deleted", true);
        return result;
    }

    private boolean canViewResource(Long currentUserId, Resource resource, Long requestBookId) {
        int visibility = resource.getVisibility() == null ? 1 : resource.getVisibility();
        if (visibility == 1) {
            return true;
        }
        if (currentUserId == null) {
            return false;
        }
        if (currentUserId.equals(resource.getUserId())) {
            return true;
        }
        if (visibility == 3) {
            return false;
        }
        Long bookId = resource.getBookId() != null ? resource.getBookId() : requestBookId;
        if (bookId == null) {
            return false;
        }
        return bookOrderMapper.selectCountByQuery(
            QueryWrapper.create()
                .where("book_id = ?", bookId)
                .and("buyer_id = ?", currentUserId)
                .and("status >= 2")
                .and("status <> 5")
                .and("is_deleted = 0")
        ) > 0;
    }

    private void validateResourceOwnership(Resource resource, Long currentUserId) {
        if ("book".equals(resource.getBindType()) && resource.getBindId() != null) {
            Book book = bookMapper.selectOneById(resource.getBindId());
            if (book == null || isDeleted(book.getIsDeleted()) || !currentUserId.equals(book.getUserId())) {
                throw new CustomerException("403", "无权绑定到该书籍");
            }
        }
        if ("pathNode".equals(resource.getBindType()) && resource.getBindId() != null) {
            PathNode node = pathNodeMapper.selectOneById(resource.getBindId());
            if (node == null || isDeleted(node.getIsDeleted())) {
                throw new CustomerException("404", "路径节点不存在");
            }
            LearningPath path = learningPathMapper.selectOneById(node.getPathId());
            if (path == null || isDeleted(path.getIsDeleted()) || !currentUserId.equals(path.getUserId())) {
                throw new CustomerException("403", "无权绑定到该路径节点");
            }
        }
    }

    private void syncPathNodeBinding(Resource resource, Map<String, Object> previousBinding) {
        if (previousBinding != null && "pathNode".equals(previousBinding.get("bindType")) && previousBinding.get("bindId") != null) {
            removeResourceFromPathNode(resource.getId(), asLong(previousBinding.get("bindId")));
        }
        if (!isDeleted(resource.getIsDeleted()) && "pathNode".equals(resource.getBindType()) && resource.getBindId() != null) {
            addResourceToPathNode(resource.getId(), resource.getBindId());
        }
    }

    private Map<String, Object> bindingSnapshot(String bindType, Long bindId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("bindType", defaultString(bindType, ""));
        snapshot.put("bindId", bindId);
        return snapshot;
    }

    private void addResourceToPathNode(Long resourceId, Long pathNodeId) {
        PathNode node = pathNodeMapper.selectOneById(pathNodeId);
        if (node == null || isDeleted(node.getIsDeleted())) {
            return;
        }
        Set<Long> ids = new LinkedHashSet<>(splitIds(node.getResourceIds()));
        ids.add(resourceId);
        node.setResourceIds(toJsonIdArray(ids));
        node.setUpdateTime(LocalDateTime.now());
        pathNodeMapper.update(node);
    }

    private void removeResourceFromPathNode(Long resourceId, Long pathNodeId) {
        PathNode node = pathNodeMapper.selectOneById(pathNodeId);
        if (node == null || isDeleted(node.getIsDeleted())) {
            return;
        }
        Set<Long> ids = new LinkedHashSet<>(splitIds(node.getResourceIds()));
        ids.remove(resourceId);
        node.setResourceIds(toJsonIdArray(ids));
        node.setUpdateTime(LocalDateTime.now());
        pathNodeMapper.update(node);
    }

    private Long resolveBookId(Map<String, Object> payload) {
        Long bookId = asLong(payload.get("bookId"));
        String bindType = resolveBindType(payload);
        if ("none".equals(bindType)) {
            return null;
        }
        if ("book".equals(bindType)) {
            return asLong(payload.get("bindId")) != null ? asLong(payload.get("bindId")) : bookId;
        }
        if ("pathNode".equals(bindType) && bookId == null) {
            Long bindId = asLong(payload.get("bindId"));
            PathNode node = bindId == null ? null : pathNodeMapper.selectOneById(bindId);
            if (node != null && !isDeleted(node.getIsDeleted()) && node.getPathId() != null) {
                LearningPath path = learningPathMapper.selectOneById(node.getPathId());
                if (path != null && !isDeleted(path.getIsDeleted()) && path.getBookId() != null) {
                    return path.getBookId();
                }
            }
        }
        return bookId;
    }

    private String resolveBindType(Map<String, Object> payload) {
        String bindType = defaultString(payload.get("bindType"), "");
        if (bindType.isBlank()) {
            return payload.get("bindId") == null ? "none" : "book";
        }
        return bindType;
    }

    private Map<String, Object> toResourceItem(Resource resource) {
        String rawFileUrl = defaultString(resource.getFileUrl(), "");
        String fileUrl = normalizeResourceUrl(rawFileUrl);
        String fileFormat = defaultString(resource.getFileFormat(), "");
        String bindType = defaultString(resource.getBindType(), "none");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", resource.getId());
        item.put("name", defaultString(resource.getTitle(), "未命名资源"));
        item.put("title", defaultString(resource.getTitle(), "未命名资源"));
        item.put("description", defaultString(resource.getDescription(), ""));
        item.put("type", resource.getType());
        item.put("typeLabel", mapType(resource.getType()));
        item.put("fileUrl", fileUrl);
        item.put("rawFileUrl", rawFileUrl);
        item.put("fileFormat", fileFormat);
        item.put("fileSize", resource.getFileSize() == null ? 0L : resource.getFileSize());
        item.put("fileSizeLabel", formatFileSize(resource.getFileSize()));
        item.put("previewType", inferPreviewType(fileUrl, fileFormat));
        item.put("downloadCount", resource.getDownloadCount() == null ? 0 : resource.getDownloadCount());
        item.put("visibility", resource.getVisibility() == null ? 1 : resource.getVisibility());
        item.put("visibilityLabel", mapVisibility(resource.getVisibility()));
        item.put("bindType", bindType);
        item.put("bindTypeLabel", mapBindType(bindType));
        item.put("bindId", resource.getBindId());
        item.put("bookId", resource.getBookId());
        item.put("bindTargetTitle", resolveBindTargetTitle(resource));
        item.put("bindingSummary", buildBindingSummary(resource));
        item.put("canEdit", AuthContext.getUserId() != null && AuthContext.getUserId().equals(resource.getUserId()));
        item.put("createTime", resource.getCreateTime());
        item.put("updateTime", resource.getUpdateTime());
        return item;
    }

    private List<Long> splitIds(String resourceIds) {
        List<Long> ids = new ArrayList<>();
        if (resourceIds == null || resourceIds.isBlank()) {
            return ids;
        }
        String normalized = resourceIds.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace("\"", "").replace("'", "");
        if (normalized.isBlank()) {
            return ids;
        }
        for (String part : normalized.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private String toJsonIdArray(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        return ids.stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
    }

    private int normalizeVisibility(int visibility) {
        return switch (visibility) {
            case 1, 2, 3 -> visibility;
            default -> 1;
        };
    }

    private String mapType(Integer type) {
        int value = type == null ? 0 : type;
        return switch (value) {
            case 1 -> "课件";
            case 2 -> "习题";
            case 3 -> "笔记";
            case 4 -> "拓展阅读";
            default -> "其他";
        };
    }

    private String mapVisibility(Integer visibility) {
        int value = visibility == null ? 1 : visibility;
        return switch (value) {
            case 2 -> "仅买家可见";
            case 3 -> "私密";
            default -> "公开";
        };
    }

    private String mapBindType(String bindType) {
        return switch (defaultString(bindType, "none")) {
            case "book" -> "关联书籍";
            case "pathNode" -> "关联路径节点";
            default -> "未绑定";
        };
    }

    private String resolveBindTargetTitle(Resource resource) {
        String bindType = defaultString(resource.getBindType(), "none");
        Long bindId = resource.getBindId();
        if ("book".equals(bindType)) {
            Long bookId = bindId == null ? resource.getBookId() : bindId;
            Book book = bookId == null ? null : bookMapper.selectOneById(bookId);
            return book == null ? "" : defaultString(book.getTitle(), "");
        }
        if ("pathNode".equals(bindType) && bindId != null) {
            PathNode node = pathNodeMapper.selectOneById(bindId);
            if (node == null || isDeleted(node.getIsDeleted())) {
                return "";
            }
            LearningPath path = node.getPathId() == null ? null : learningPathMapper.selectOneById(node.getPathId());
            String pathTitle = path == null ? "" : defaultString(path.getTitle(), "");
            String nodeTitle = defaultString(node.getTitle(), "路径节点");
            return pathTitle.isBlank() ? nodeTitle : pathTitle + " / " + nodeTitle;
        }
        return "";
    }

    private String buildBindingSummary(Resource resource) {
        String label = mapBindType(resource.getBindType());
        String title = resolveBindTargetTitle(resource);
        Long bindId = resource.getBindId();
        if (bindId == null && "book".equals(defaultString(resource.getBindType(), "none"))) {
            bindId = resource.getBookId();
        }
        String idText = bindId == null ? "" : " #" + bindId;
        return title.isBlank() ? label + idText : label + idText + " · " + title;
    }

    private String inferPreviewType(String fileUrl, String fileFormat) {
        String text = (defaultString(fileFormat, "") + " " + defaultString(fileUrl, "")).toLowerCase();
        if (List.of("jpg", "jpeg", "png", "gif", "webp", "bmp").stream().anyMatch(text::contains)) {
            return "image";
        }
        if (List.of("mp4", "mov", "m4v", "webm", "avi").stream().anyMatch(text::contains)) {
            return "video";
        }
        if (List.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx").stream().anyMatch(text::contains)) {
            return "document";
        }
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return "link";
        }
        return "file";
    }

    private String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "未知大小";
        }
        double value = size;
        if (value < 1024) {
            return size + "B";
        }
        value = value / 1024;
        if (value < 1024) {
            return String.format("%.1fKB", value);
        }
        value = value / 1024;
        if (value < 1024) {
            return String.format("%.1fMB", value);
        }
        return String.format("%.1fGB", value / 1024);
    }

    private String resolveExtension(String originalFilename) {
        String fileName = defaultString(originalFilename, "");
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return ".dat";
        }
        String extension = fileName.substring(index).toLowerCase();
        if (extension.length() > 10 || extension.contains("/") || extension.contains("\\")) {
            return ".dat";
        }
        return extension;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String text = defaultString(baseUrl, "/");
        return text.endsWith("/") ? text : text + "/";
    }

    private String normalizeResourceUrl(String fileUrl) {
        String text = defaultString(fileUrl, "");
        if (text.isBlank()) {
            return "";
        }
        String lower = text.toLowerCase();
        if (lower.startsWith("http://")
            || lower.startsWith("https://")
            || lower.startsWith("wxfile://")
            || lower.startsWith("cloud://")
            || lower.startsWith("data:")) {
            return text;
        }
        String baseUrl = normalizeBaseUrl(imageBaseUrl);
        return text.startsWith("/") ? baseUrl + text.substring(1) : baseUrl + text;
    }

    private boolean isDeleted(Integer isDeleted) {
        return isDeleted != null && isDeleted == 1;
    }

    private String defaultString(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
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
