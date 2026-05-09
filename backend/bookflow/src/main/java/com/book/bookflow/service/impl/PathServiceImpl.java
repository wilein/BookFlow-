package com.book.bookflow.service.impl;

import com.alibaba.fastjson.JSON;
import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.LearningPath;
import com.book.bookflow.entity.PathNode;
import com.book.bookflow.entity.Resource;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserPathNodeProgress;
import com.book.bookflow.entity.UserPathProgress;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.LearningPathMapper;
import com.book.bookflow.mapper.PathNodeMapper;
import com.book.bookflow.mapper.ResourceMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.mapper.UserPathNodeProgressMapper;
import com.book.bookflow.mapper.UserPathProgressMapper;
import com.book.bookflow.service.PathService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PathServiceImpl implements PathService {

    private final LearningPathMapper learningPathMapper;
    private final PathNodeMapper pathNodeMapper;
    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;
    private final UserPathProgressMapper userPathProgressMapper;
    private final UserPathNodeProgressMapper userPathNodeProgressMapper;

    @Value("${app.image-base-url:}")
    private String imageBaseUrl;

    public PathServiceImpl(LearningPathMapper learningPathMapper,
                           PathNodeMapper pathNodeMapper,
                           ResourceMapper resourceMapper,
                           UserMapper userMapper,
                           UserPathProgressMapper userPathProgressMapper,
                           UserPathNodeProgressMapper userPathNodeProgressMapper) {
        this.learningPathMapper = learningPathMapper;
        this.pathNodeMapper = pathNodeMapper;
        this.resourceMapper = resourceMapper;
        this.userMapper = userMapper;
        this.userPathProgressMapper = userPathProgressMapper;
        this.userPathNodeProgressMapper = userPathNodeProgressMapper;
    }

    @Override
    public List<Map<String, Object>> getPublicPaths(String category, String keyword) {
        Long currentUserId = AuthContext.getUserId();
        List<LearningPath> paths = learningPathMapper.selectListByQuery(
            QueryWrapper.create()
                .where("status = 1")
                .and("is_deleted = 0")
                .orderBy("id desc")
                .limit(200)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (LearningPath path : paths) {
            String inferredCategory = inferCategory(path);
            if (!matchesCategory(category, inferredCategory)) {
                continue;
            }
            if (!matchesKeyword(path, keyword)) {
                continue;
            }
            result.add(toPathListItem(path, currentUserId, null, false));
        }
        return result;
    }

    @Override
    public Map<String, Object> getPathDetail(Long pathId) {
        LearningPath path = requirePath(pathId);
        Long currentUserId = AuthContext.getUserId();
        boolean isCreator = currentUserId != null && currentUserId.equals(path.getUserId());
        if (!isCreator && !Objects.equals(path.getStatus(), 1)) {
            throw new CustomerException("403", "当前路径不可查看");
        }

        User creator = path.getUserId() == null ? null : userMapper.selectOneById(path.getUserId());
        List<PathNode> nodes = pathNodeMapper.selectListByQuery(
            QueryWrapper.create().where("path_id = ?", pathId).and("is_deleted = 0").orderBy("order_num asc, id asc")
        );
        Set<Long> activeNodeIds = nodes.stream()
            .map(PathNode::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        UserPathProgress progress = currentUserId == null ? null : userPathProgressMapper.selectOneByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("path_id = ?", pathId).and("is_deleted = 0").limit(1)
        );

        Set<Long> completedNodeIds = new LinkedHashSet<>();
        if (currentUserId != null) {
            List<UserPathNodeProgress> progresses = userPathNodeProgressMapper.selectListByQuery(
                QueryWrapper.create()
                    .where("user_id = ?", currentUserId)
                    .and("path_id = ?", pathId)
                    .and("completed = 1")
                    .and("is_deleted = 0")
            );
            for (UserPathNodeProgress item : progresses) {
                if (item.getNodeId() != null && activeNodeIds.contains(item.getNodeId())) {
                    completedNodeIds.add(item.getNodeId());
                }
            }
        }

        Long copiedPathId = currentUserId == null ? null : findCopiedPathId(currentUserId, pathId);
        List<Map<String, Object>> nodeItems = new ArrayList<>();
        for (PathNode node : nodes) {
            List<Map<String, Object>> resources = toNodeResourceItems(node, currentUserId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", node.getId());
            item.put("title", defaultString(node.getTitle(), "未命名节点"));
            item.put("description", defaultString(node.getDescription(), ""));
            item.put("duration", formatMinutes(node.getEstimatedMinutes()));
            item.put("completed", node.getId() != null && completedNodeIds.contains(node.getId()));
            item.put("level", node.getParentId() == null || node.getParentId() == 0 ? 1 : 2);
            item.put("resourceCount", resources.size());
            item.put("resourceIds", splitIds(node.getResourceIds()));
            item.put("resources", resources);
            item.put("learningGoal", defaultString(node.getLearningGoal(), buildLearningGoal(node)));
            item.put("learningMethod", defaultString(node.getLearningMethod(), buildLearningMethod(resources)));
            item.put("deliverable", defaultString(node.getDeliverable(), buildDeliverable(node)));
            item.put("learningSteps", parseLearningSteps(node, resources));
            nodeItems.add(item);
        }

        int totalNodeCount = nodeItems.size();
        int completedCount = completedNodeIds.size();
        int progressPercent = totalNodeCount == 0 ? 0 : (int) Math.round(completedCount * 100.0 / totalNodeCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", path.getId());
        result.put("sourcePathId", path.getSourcePathId());
        result.put("title", defaultString(path.getTitle(), "学习路径"));
        result.put("creator", creator != null ? defaultString(creator.getNickname(), "路径创建者") : "路径创建者");
        result.put("difficulty", mapDifficultyLabel(path.getDifficulty()));
        result.put("totalDuration", formatHours(path.getEstimatedHours()));
        result.put("description", defaultString(path.getDescription(), ""));
        result.put("coverImage", defaultString(path.getCoverImage(), ""));
        result.put("coverImageStatus", path.getCoverImageStatus() == null ? 0 : path.getCoverImageStatus());
        result.put("coverImageStatusLabel", mapCoverImageStatus(path.getCoverImageStatus()));
        result.put("status", path.getStatus());
        result.put("statusLabel", mapPathStatus(path.getStatus()));
        result.put("isCreator", isCreator);
        result.put("started", progress != null);
        result.put("copied", copiedPathId != null);
        result.put("copiedPathId", copiedPathId);
        result.put("canEdit", isCreator);
        result.put("canManage", isCreator);
        result.put("canCopy", currentUserId != null && !isCreator && Objects.equals(path.getStatus(), 1));
        result.put("completedCount", completedCount);
        result.put("completedNodeIds", new ArrayList<>(completedNodeIds));
        result.put("progressPercent", progress != null && progress.getProgressPercent() != null ? progress.getProgressPercent() : progressPercent);
        result.put("nodes", nodeItems);
        return result;
    }

    @Override
    public Map<String, Object> saveDraft(Map<String, Object> payload) {
        return savePath(payload, 0);
    }

    @Override
    public Map<String, Object> publishPath(Map<String, Object> payload) {
        return savePath(payload, 2);
    }

    @Override
    public Map<String, Object> uploadPathCover(MultipartFile file) {
        Long currentUserId = AuthContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new CustomerException("400", "请选择路径封面图");
        }
        String contentType = defaultString(file.getContentType(), "").toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new CustomerException("400", "路径封面只支持图片文件");
        }
        if (file.getSize() > 10 * 1024 * 1024L) {
            throw new CustomerException("400", "路径封面不能超过10MB");
        }

        String extension = resolveImageExtension(file.getOriginalFilename());
        String fileName = currentUserId + "_" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDirectory = Paths.get("uploads", "path");
        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetDirectory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new CustomerException("500", "路径封面上传失败");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", normalizeBaseUrl(imageBaseUrl) + "uploads/path/" + fileName);
        result.put("fileName", fileName);
        result.put("auditStatus", 1);
        result.put("auditStatusLabel", "待审核");
        return result;
    }

    @Override
    public Map<String, Object> startLearning(Long pathId) {
        Long currentUserId = AuthContext.requireUserId();
        LearningPath path = requirePath(pathId);
        LocalDateTime now = LocalDateTime.now();
        UserPathProgress progress = findAnyProgress(currentUserId, pathId);
        if (progress == null) {
            progress = UserPathProgress.builder()
                .userId(currentUserId)
                .pathId(pathId)
                .progressPercent(0)
                .completedCount(0)
                .startedAt(now)
                .lastLearnTime(now)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            userPathProgressMapper.insert(progress);
        } else {
            if (isDeleted(progress.getIsDeleted())) {
                progress.setProgressPercent(0);
                progress.setCompletedCount(0);
                progress.setStartedAt(now);
            }
            progress.setLastLearnTime(now);
            progress.setUpdateTime(now);
            progress.setIsDeleted(0);
            userPathProgressMapper.update(progress);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", path.getId());
        result.put("started", true);
        result.put("progressPercent", progress.getProgressPercent() == null ? 0 : progress.getProgressPercent());
        result.put("completedCount", progress.getCompletedCount() == null ? 0 : progress.getCompletedCount());
        return result;
    }

    @Override
    public Map<String, Object> cancelLearning(Long pathId) {
        Long currentUserId = AuthContext.requireUserId();
        LearningPath path = requirePath(pathId);
        LocalDateTime now = LocalDateTime.now();

        List<UserPathProgress> progresses = userPathProgressMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("path_id = ?", pathId)
                .and("is_deleted = 0")
        );
        for (UserPathProgress progress : progresses) {
            progress.setIsDeleted(1);
            progress.setUpdateTime(now);
            userPathProgressMapper.update(progress);
        }

        List<UserPathNodeProgress> nodeProgresses = userPathNodeProgressMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("path_id = ?", pathId)
                .and("is_deleted = 0")
        );
        for (UserPathNodeProgress nodeProgress : nodeProgresses) {
            nodeProgress.setCompleted(0);
            nodeProgress.setCompletedTime(null);
            nodeProgress.setIsDeleted(1);
            nodeProgress.setUpdateTime(now);
            userPathNodeProgressMapper.update(nodeProgress);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", path.getId());
        result.put("started", false);
        result.put("progressPercent", 0);
        result.put("completedCount", 0);
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyLearningPaths() {
        Long currentUserId = AuthContext.requireUserId();
        List<UserPathProgress> progresses = userPathProgressMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("is_deleted = 0")
                .orderBy("last_learn_time desc, id desc")
                .limit(100)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> seenPathIds = new LinkedHashSet<>();
        for (UserPathProgress progress : progresses) {
            Long pathId = progress.getPathId();
            if (pathId == null || seenPathIds.contains(pathId)) {
                continue;
            }
            LearningPath path = learningPathMapper.selectOneById(pathId);
            if (!isVisibleToUser(path, currentUserId)) {
                continue;
            }
            seenPathIds.add(pathId);
            result.add(toPathListItem(path, currentUserId, progress, true));
        }
        return result;
    }

    @Override
    public Map<String, Object> getCurrentLearningPath() {
        List<Map<String, Object>> paths = getMyLearningPaths();
        if (paths.isEmpty()) {
            return null;
        }
        for (Map<String, Object> path : paths) {
            Object progress = path.get("progressPercent");
            if (progress instanceof Number number && number.intValue() < 100) {
                return path;
            }
        }
        return paths.get(0);
    }

    @Override
    public Map<String, Object> completeNode(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long pathId = asLong(payload.get("pathId"));
        Long nodeId = asLong(payload.get("nodeId"));
        boolean completed = parseBoolean(payload.get("completed"), true);
        if (pathId == null || nodeId == null) {
            throw new CustomerException("400", "节点参数错误");
        }
        requirePath(pathId);
        PathNode node = pathNodeMapper.selectOneById(nodeId);
        if (node == null || isDeleted(node.getIsDeleted()) || !pathId.equals(node.getPathId())) {
            throw new CustomerException("404", "节点不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        UserPathProgress progress = findAnyProgress(currentUserId, pathId);
        if (progress == null) {
            progress = UserPathProgress.builder()
                .userId(currentUserId)
                .pathId(pathId)
                .progressPercent(0)
                .completedCount(0)
                .startedAt(now)
                .lastLearnTime(now)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            userPathProgressMapper.insert(progress);
        } else if (isDeleted(progress.getIsDeleted())) {
            progress.setProgressPercent(0);
            progress.setCompletedCount(0);
            progress.setStartedAt(now);
            progress.setLastLearnTime(now);
            progress.setUpdateTime(now);
            progress.setIsDeleted(0);
            userPathProgressMapper.update(progress);
        }

        UserPathNodeProgress nodeProgress = userPathNodeProgressMapper.selectOneByQuery(
            QueryWrapper.create().where("user_id = ?", currentUserId).and("path_id = ?", pathId).and("node_id = ?", nodeId).limit(1)
        );
        if (nodeProgress == null) {
            nodeProgress = UserPathNodeProgress.builder()
                .userId(currentUserId)
                .pathId(pathId)
                .nodeId(nodeId)
                .completed(completed ? 1 : 0)
                .completedTime(completed ? now : null)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            userPathNodeProgressMapper.insert(nodeProgress);
        } else {
            nodeProgress.setCompleted(completed ? 1 : 0);
            nodeProgress.setCompletedTime(completed ? now : null);
            nodeProgress.setUpdateTime(now);
            nodeProgress.setIsDeleted(0);
            userPathNodeProgressMapper.update(nodeProgress);
        }

        long totalNodeCount = pathNodeMapper.selectCountByQuery(
            QueryWrapper.create().where("path_id = ?", pathId).and("is_deleted = 0")
        );
        Set<Long> activeNodeIds = pathNodeMapper.selectListByQuery(
                QueryWrapper.create().where("path_id = ?", pathId).and("is_deleted = 0")
            )
            .stream()
            .map(PathNode::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        long completedCount = userPathNodeProgressMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("path_id = ?", pathId)
                .and("completed = 1")
                .and("is_deleted = 0")
        ).stream().filter(item -> item.getNodeId() != null && activeNodeIds.contains(item.getNodeId())).count();
        int progressPercent = totalNodeCount == 0 ? 0 : (int) Math.round(completedCount * 100.0 / totalNodeCount);

        progress.setCompletedCount((int) completedCount);
        progress.setProgressPercent(progressPercent);
        progress.setLastLearnTime(now);
        progress.setUpdateTime(now);
        userPathProgressMapper.update(progress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", pathId);
        result.put("nodeId", nodeId);
        result.put("completed", completed);
        result.put("completedCount", completedCount);
        result.put("progressPercent", progressPercent);
        return result;
    }

    @Override
    public Map<String, Object> copyPath(Long pathId) {
        Long currentUserId = AuthContext.requireUserId();
        LearningPath source = requirePath(pathId);
        if (currentUserId.equals(source.getUserId())) {
            throw new CustomerException("400", "自己的路径无需复制");
        }
        Long copiedPathId = findCopiedPathId(currentUserId, pathId);
        if (copiedPathId != null) {
            Map<String, Object> existing = new LinkedHashMap<>();
            existing.put("pathId", copiedPathId);
            existing.put("copied", true);
            existing.put("exists", true);
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        LearningPath copy = LearningPath.builder()
            .userId(currentUserId)
            .bookId(null)
            .sourcePathId(source.getId())
            .title(source.getTitle() + "（我的副本）")
            .description(defaultString(source.getDescription(), ""))
            .coverImage(defaultString(source.getCoverImage(), ""))
            .coverImageStatus(source.getCoverImageStatus())
            .difficulty(source.getDifficulty())
            .estimatedHours(source.getEstimatedHours())
            .status(0)
            .viewCount(0)
            .favoriteCount(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        learningPathMapper.insert(copy);

        List<PathNode> nodes = pathNodeMapper.selectListByQuery(
            QueryWrapper.create().where("path_id = ?", source.getId()).and("is_deleted = 0").orderBy("order_num asc, id asc")
        );
        for (PathNode node : nodes) {
            PathNode copiedNode = PathNode.builder()
                .pathId(copy.getId())
                .parentId(node.getParentId())
                .title(defaultString(node.getTitle(), "未命名节点"))
                .description(defaultString(node.getDescription(), ""))
                .learningGoal(defaultString(node.getLearningGoal(), ""))
                .learningMethod(defaultString(node.getLearningMethod(), ""))
                .deliverable(defaultString(node.getDeliverable(), ""))
                .stepsJson(defaultString(node.getStepsJson(), "[]"))
                .estimatedMinutes(node.getEstimatedMinutes())
                .orderNum(node.getOrderNum())
                .resourceIds(filterPublicResourceIds(node.getResourceIds()))
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            pathNodeMapper.insert(copiedNode);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", copy.getId());
        result.put("copied", true);
        result.put("status", copy.getStatus());
        return result;
    }

    @Override
    public Map<String, Object> changePathStatus(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        LearningPath path = requirePath(asLong(payload.get("pathId")));
        if (!currentUserId.equals(path.getUserId())) {
            throw new CustomerException("403", "无权管理该路径");
        }
        int status = normalizeStatus(asInteger(payload.get("status"), path.getStatus() == null ? 0 : path.getStatus()));
        path.setStatus(status);
        if (!defaultString(path.getCoverImage(), "").isBlank()) {
            if (status == 1) {
                path.setCoverImageStatus(2);
            } else if (status == 2) {
                path.setCoverImageStatus(1);
            }
        }
        path.setUpdateTime(LocalDateTime.now());
        learningPathMapper.update(path);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", path.getId());
        result.put("status", status);
        result.put("statusLabel", mapPathStatus(status));
        return result;
    }

    private Map<String, Object> savePath(Map<String, Object> payload, int status) {
        Long currentUserId = AuthContext.requireUserId();
        LocalDateTime now = LocalDateTime.now();
        Long pathId = asLong(payload.get("id"));
        LearningPath path = pathId == null ? null : learningPathMapper.selectOneById(pathId);
        boolean isNew = path == null;
        if (isNew) {
            path = new LearningPath();
            path.setCreateTime(now);
            path.setUserId(currentUserId);
            path.setViewCount(0);
            path.setFavoriteCount(0);
            path.setIsDeleted(0);
            path.setSourcePathId(asLong(payload.get("sourcePathId")));
        } else if (!currentUserId.equals(path.getUserId())) {
            throw new CustomerException("403", "无权编辑该路径");
        }

        String previousCover = defaultString(path.getCoverImage(), "");
        String nextCover = defaultString(payload.get("cover"), defaultString(payload.get("coverImage"), ""));
        int normalizedStatus = normalizeStatus(status);

        path.setTitle(defaultString(payload.get("title"), "未命名路径"));
        path.setDescription(defaultString(payload.get("description"), ""));
        path.setDifficulty(mapDifficultyValue(defaultString(payload.get("difficulty"), "入门")));
        path.setEstimatedHours(parseLeadingInt(defaultString(payload.get("totalDuration"), "0")));
        path.setCoverImage(nextCover);
        path.setCoverImageStatus(resolveCoverImageStatus(path.getCoverImageStatus(), previousCover, nextCover, normalizedStatus));
        path.setBookId(asLong(payload.get("bookId")));
        path.setStatus(normalizedStatus);
        path.setUpdateTime(now);

        Map<Long, PathNode> existingNodes = new LinkedHashMap<>();
        if (!isNew) {
            List<PathNode> nodes = pathNodeMapper.selectListByQuery(
                QueryWrapper.create().where("path_id = ?", path.getId()).and("is_deleted = 0")
            );
            for (PathNode node : nodes) {
                if (node.getId() != null) {
                    existingNodes.put(node.getId(), node);
                }
            }
        }

        if (isNew) {
            learningPathMapper.insert(path);
        } else {
            learningPathMapper.update(path);
        }

        Set<Long> retainedNodeIds = new LinkedHashSet<>();
        Object rawNodes = payload.get("nodes");
        if (rawNodes instanceof List<?> nodeList) {
            int orderNum = 1;
            for (Object rawNode : nodeList) {
                if (!(rawNode instanceof Map<?, ?> nodeMap)) {
                    continue;
                }
                Long nodeId = asLong(nodeMap.get("id"));
                PathNode node = nodeId == null ? null : existingNodes.get(nodeId);
                boolean creatingNode = node == null;
                if (creatingNode) {
                    node = new PathNode();
                    node.setPathId(path.getId());
                    node.setCreateTime(now);
                }
                node.setParentId(parseParentId(nodeMap.get("level")));
                node.setTitle(defaultString(nodeMap.get("title"), "未命名节点"));
                node.setDescription(defaultString(nodeMap.get("description"), ""));
                node.setLearningGoal(defaultString(nodeMap.get("learningGoal"), ""));
                node.setLearningMethod(defaultString(nodeMap.get("learningMethod"), ""));
                node.setDeliverable(defaultString(nodeMap.get("deliverable"), ""));
                node.setStepsJson(toStepsJson(nodeMap.get("learningSteps")));
                node.setOrderNum(orderNum++);
                node.setEstimatedMinutes(parseLeadingInt(defaultString(nodeMap.get("duration"), "0")));
                node.setResourceIds(joinIds(nodeMap.get("resourceIds")));
                node.setUpdateTime(now);
                node.setIsDeleted(0);
                if (creatingNode) {
                    pathNodeMapper.insert(node);
                } else {
                    pathNodeMapper.update(node);
                }
                if (node.getId() != null) {
                    retainedNodeIds.add(node.getId());
                }
            }
        }
        if (!isNew) {
            for (PathNode existingNode : existingNodes.values()) {
                if (existingNode.getId() != null && !retainedNodeIds.contains(existingNode.getId())) {
                    existingNode.setIsDeleted(1);
                    existingNode.setUpdateTime(now);
                    pathNodeMapper.update(existingNode);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", path.getId());
        result.put("status", path.getStatus());
        result.put("statusLabel", mapPathStatus(path.getStatus()));
        return result;
    }

    private Map<String, Object> toPathListItem(LearningPath path,
                                               Long currentUserId,
                                               UserPathProgress suppliedProgress,
                                               boolean forceStarted) {
        User creator = path.getUserId() == null ? null : userMapper.selectOneById(path.getUserId());
        long nodeCount = countNodes(path.getId());
        long learnerCount = countLearners(path.getId());
        UserPathProgress progress = suppliedProgress != null ? suppliedProgress : findProgress(currentUserId, path.getId());
        int progressPercent = progress == null || progress.getProgressPercent() == null ? 0 : progress.getProgressPercent();
        int completedCount = progress == null || progress.getCompletedCount() == null ? 0 : progress.getCompletedCount();
        String title = defaultString(path.getTitle(), "学习路径");

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", path.getId());
        item.put("pathId", path.getId());
        item.put("sourcePathId", path.getSourcePathId());
        item.put("title", title);
        item.put("name", title);
        item.put("description", defaultString(path.getDescription(), ""));
        item.put("creator", creator != null ? defaultString(creator.getNickname(), "校园同学") : "校园同学");
        item.put("difficulty", mapDifficultyLabel(path.getDifficulty()));
        item.put("totalDuration", formatHours(path.getEstimatedHours()));
        item.put("coverImage", defaultString(path.getCoverImage(), ""));
        item.put("cover", defaultString(path.getCoverImage(), ""));
        item.put("coverImageStatus", path.getCoverImageStatus() == null ? 0 : path.getCoverImageStatus());
        item.put("coverImageStatusLabel", mapCoverImageStatus(path.getCoverImageStatus()));
        item.put("status", path.getStatus());
        item.put("statusLabel", mapPathStatus(path.getStatus()));
        item.put("category", inferCategory(path));
        item.put("nodeCount", nodeCount);
        item.put("bookCount", nodeCount);
        item.put("learnerCount", learnerCount);
        item.put("learners", learnerCount);
        item.put("started", forceStarted || progress != null);
        item.put("progressPercent", progressPercent);
        item.put("completedCount", completedCount);
        item.put("startedAt", progress == null ? "" : formatTime(progress.getStartedAt()));
        item.put("lastLearnTime", progress == null ? "" : formatTime(progress.getLastLearnTime()));
        item.put("routeUrl", "/pages/path/detail?pathId=" + path.getId());
        item.put("isCreator", currentUserId != null && currentUserId.equals(path.getUserId()));
        return item;
    }

    private UserPathProgress findProgress(Long userId, Long pathId) {
        if (userId == null || pathId == null) {
            return null;
        }
        return userPathProgressMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", userId)
                .and("path_id = ?", pathId)
                .and("is_deleted = 0")
                .limit(1)
        );
    }

    private UserPathProgress findAnyProgress(Long userId, Long pathId) {
        if (userId == null || pathId == null) {
            return null;
        }
        return userPathProgressMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", userId)
                .and("path_id = ?", pathId)
                .limit(1)
        );
    }

    private long countNodes(Long pathId) {
        return pathNodeMapper.selectCountByQuery(
            QueryWrapper.create().where("path_id = ?", pathId).and("is_deleted = 0")
        );
    }

    private long countLearners(Long pathId) {
        return userPathProgressMapper.selectCountByQuery(
            QueryWrapper.create().where("path_id = ?", pathId).and("is_deleted = 0")
        );
    }

    private boolean isVisibleToUser(LearningPath path, Long userId) {
        if (path == null || isDeleted(path.getIsDeleted())) {
            return false;
        }
        return Objects.equals(path.getStatus(), 1) || (userId != null && userId.equals(path.getUserId()));
    }

    private boolean matchesCategory(String selectedCategory, String category) {
        String selected = defaultString(selectedCategory, "全部");
        return selected.isBlank() || "全部".equals(selected) || selected.equals(category);
    }

    private boolean matchesKeyword(LearningPath path, String keyword) {
        String normalizedKeyword = normalizeSearchText(keyword);
        if (normalizedKeyword.isBlank()) {
            return true;
        }
        String source = normalizeSearchText(defaultString(path.getTitle(), "") + " " + defaultString(path.getDescription(), ""));
        return source.contains(normalizedKeyword);
    }

    private String inferCategory(LearningPath path) {
        String source = normalizeSearchText(defaultString(path.getTitle(), "") + " " + defaultString(path.getDescription(), ""));
        if (containsAny(source, "java", "spring", "python", "vue", "react", "前端", "后端", "开发", "编程")) {
            return "编程开发";
        }
        if (containsAny(source, "数据结构", "算法", "数据库", "操作系统", "计算机网络", "计算机")) {
            return "计算机基础";
        }
        if (containsAny(source, "考研", "数学", "英语", "政治", "专业课")) {
            return "考研课程";
        }
        if (containsAny(source, "设计", "产品", "交互", "ui", "视觉")) {
            return "设计产品";
        }
        if (containsAny(source, "文学", "语言", "阅读", "写作", "翻译")) {
            return "语言文学";
        }
        return "其他";
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(normalizeSearchText(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSearchText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private LearningPath requirePath(Long pathId) {
        if (pathId == null) {
            throw new CustomerException("400", "路径参数错误");
        }
        LearningPath path = learningPathMapper.selectOneById(pathId);
        if (path == null || isDeleted(path.getIsDeleted())) {
            throw new CustomerException("404", "路径不存在");
        }
        return path;
    }

    private Long findCopiedPathId(Long userId, Long sourcePathId) {
        if (userId == null || sourcePathId == null) {
            return null;
        }
        LearningPath copied = learningPathMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", userId)
                .and("source_path_id = ?", sourcePathId)
                .and("is_deleted = 0")
                .limit(1)
        );
        return copied == null ? null : copied.getId();
    }

    private String filterPublicResourceIds(String resourceIds) {
        List<Long> ids = splitIds(resourceIds);
        List<Long> publicIds = ids.stream()
            .map(resourceMapper::selectOneById)
            .filter(resource -> resource != null && !isDeleted(resource.getIsDeleted()) && Objects.equals(resource.getVisibility(), 1))
            .map(Resource::getId)
            .collect(Collectors.toList());
        return toJsonIdArray(publicIds);
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

    private String joinIds(Object resourceIds) {
        if (resourceIds instanceof List<?> list) {
            List<Long> ids = new ArrayList<>();
            for (Object item : list) {
                Long id = asLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
            return toJsonIdArray(ids);
        }
        return toJsonIdArray(splitIds(defaultString(resourceIds, "")));
    }

    private String toJsonIdArray(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        return ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
    }

    private List<Map<String, Object>> toNodeResourceItems(PathNode node, Long currentUserId) {
        Set<Long> resourceIds = new LinkedHashSet<>(splitIds(node.getResourceIds()));
        if (node.getId() != null) {
            List<Resource> boundResources = resourceMapper.selectListByQuery(
                QueryWrapper.create()
                    .where("is_deleted = 0")
                    .and("bind_type = 'pathNode'")
                    .and("bind_id = ?", node.getId())
                    .orderBy("id asc")
            );
            boundResources.stream().map(Resource::getId).filter(Objects::nonNull).forEach(resourceIds::add);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Long resourceId : resourceIds) {
            Resource resource = resourceMapper.selectOneById(resourceId);
            if (resource == null || isDeleted(resource.getIsDeleted()) || !canViewNodeResource(currentUserId, resource)) {
                continue;
            }
            items.add(toResourceItem(resource, currentUserId));
        }
        return items;
    }

    private boolean canViewNodeResource(Long currentUserId, Resource resource) {
        int visibility = resource.getVisibility() == null ? 1 : resource.getVisibility();
        return visibility == 1 || (currentUserId != null && currentUserId.equals(resource.getUserId()));
    }

    private Map<String, Object> toResourceItem(Resource resource, Long currentUserId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", resource.getId());
        item.put("name", defaultString(resource.getTitle(), "未命名资源"));
        item.put("title", defaultString(resource.getTitle(), "未命名资源"));
        item.put("description", defaultString(resource.getDescription(), ""));
        item.put("type", resource.getType());
        item.put("typeLabel", mapResourceType(resource.getType()));
        item.put("fileUrl", defaultString(resource.getFileUrl(), ""));
        item.put("fileFormat", defaultString(resource.getFileFormat(), ""));
        item.put("visibility", resource.getVisibility() == null ? 1 : resource.getVisibility());
        item.put("bindType", defaultString(resource.getBindType(), "none"));
        item.put("bindId", resource.getBindId());
        item.put("bookId", resource.getBookId());
        item.put("canEdit", currentUserId != null && currentUserId.equals(resource.getUserId()));
        return item;
    }

    private String mapResourceType(Integer type) {
        int value = type == null ? 0 : type;
        return switch (value) {
            case 1 -> "课件";
            case 2 -> "习题";
            case 3 -> "笔记";
            case 4 -> "拓展阅读";
            default -> "其他";
        };
    }

    private String buildLearningGoal(PathNode node) {
        return "理解并掌握「" + defaultString(node.getTitle(), "当前节点") + "」的核心概念，能够用自己的话复述重点，并能完成对应练习。";
    }

    private String buildLearningMethod(List<Map<String, Object>> resources) {
        if (resources == null || resources.isEmpty()) {
            return "先阅读节点说明，结合教材或课堂资料补充背景，再用笔记整理关键词、例题和疑问点。";
        }
        return "按资源顺序学习：视频先完整看一遍，PDF/课件边读边标注，练习类资源最后完成并复盘错题。";
    }

    private String buildDeliverable(PathNode node) {
        return "完成一页节点笔记，列出 3 个关键知识点、1 个实践结果和仍需追问的问题。";
    }

    private List<Map<String, Object>> buildLearningSteps(PathNode node, List<Map<String, Object>> resources) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(buildStep(1, "明确目标", "先通读节点说明，确认本节点要解决的问题，并把不熟悉的关键词记录下来。"));
        steps.add(buildStep(2, "学习资料", resources == null || resources.isEmpty()
            ? "结合教材、课堂课件或搜索资料补齐背景知识，先建立整体框架。"
            : "依次打开关联资源，视频负责建立直观理解，PDF/课件负责沉淀细节。"));
        steps.add(buildStep(3, "动手练习", "围绕「" + defaultString(node.getTitle(), "当前节点") + "」完成例题、代码、思维导图或问答练习，检查是否能独立复现。"));
        steps.add(buildStep(4, "复盘打勾", "用自己的话写下结论和易错点，确认能解释给别人听之后，再回到路径节点右侧圆圈打勾。"));
        return steps;
    }

    private Map<String, Object> buildStep(int order, String title, String content) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("order", order);
        step.put("title", title);
        step.put("content", content);
        return step;
    }

    private List<Map<String, Object>> parseLearningSteps(PathNode node, List<Map<String, Object>> resources) {
        String raw = defaultString(node.getStepsJson(), "");
        if (!raw.isBlank()) {
            try {
                List<Map> parsed = JSON.parseArray(raw, Map.class);
                if (parsed != null && !parsed.isEmpty()) {
                    List<Map<String, Object>> steps = new ArrayList<>();
                    int index = 1;
                    for (Map<?, ?> parsedStep : parsed) {
                        String title = defaultString(parsedStep.get("title"), "");
                        String content = defaultString(parsedStep.get("content"), "");
                        if (title.isBlank() && content.isBlank()) {
                            continue;
                        }
                        steps.add(buildStep(
                            asInteger(parsedStep.get("order"), index),
                            title.isBlank() ? "学习步骤" + index : title,
                            content
                        ));
                        index++;
                    }
                    if (!steps.isEmpty()) {
                        return steps;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return buildLearningSteps(node, resources);
    }

    private String toStepsJson(Object rawSteps) {
        if (!(rawSteps instanceof List<?> list) || list.isEmpty()) {
            return "[]";
        }
        List<Map<String, Object>> steps = new ArrayList<>();
        int index = 1;
        for (Object rawStep : list) {
            if (!(rawStep instanceof Map<?, ?> stepMap)) {
                continue;
            }
            String title = defaultString(stepMap.get("title"), "");
            String content = defaultString(stepMap.get("content"), "");
            if (title.isBlank() && content.isBlank()) {
                continue;
            }
            steps.add(buildStep(index++, title, content));
        }
        return JSON.toJSONString(steps);
    }

    private int resolveCoverImageStatus(Integer currentStatus, String previousCover, String nextCover, int pathStatus) {
        if (nextCover == null || nextCover.isBlank()) {
            return 0;
        }
        if (pathStatus == 1) {
            return 2;
        }
        if (pathStatus == 2) {
            return 1;
        }
        if (!Objects.equals(defaultString(previousCover, ""), defaultString(nextCover, ""))) {
            return 0;
        }
        return currentStatus == null ? 0 : currentStatus;
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

    private String resolveImageExtension(String originalFilename) {
        String fileName = defaultString(originalFilename, "");
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return ".jpg";
        }
        String extension = fileName.substring(index).toLowerCase(Locale.ROOT);
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(extension)) {
            throw new CustomerException("400", "仅支持 jpg、png、webp、gif 图片");
        }
        return extension;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String text = defaultString(baseUrl, "/");
        return text.endsWith("/") ? text : text + "/";
    }

    private String mapDifficultyLabel(Integer difficulty) {
        int value = difficulty == null ? 1 : difficulty;
        return switch (value) {
            case 2 -> "中级";
            case 3 -> "进阶";
            default -> "入门";
        };
    }

    private int mapDifficultyValue(String difficulty) {
        return switch (difficulty) {
            case "中级" -> 2;
            case "进阶" -> 3;
            default -> 1;
        };
    }

    private String mapPathStatus(Integer status) {
        int value = normalizeStatus(status == null ? 0 : status);
        return switch (value) {
            case 1 -> "已发布";
            case 2 -> "审核中";
            case 3 -> "已下架";
            default -> "草稿";
        };
    }

    private int normalizeStatus(int status) {
        return switch (status) {
            case 0, 1, 2, 3 -> status;
            default -> 0;
        };
    }

    private String formatHours(Integer hours) {
        int value = hours == null ? 0 : hours;
        return value <= 0 ? "时长待补充" : value + "小时";
    }

    private String formatMinutes(Integer minutes) {
        int value = minutes == null ? 0 : minutes;
        if (value <= 0) {
            return "时长待补充";
        }
        if (value % 60 == 0) {
            return (value / 60) + "小时";
        }
        return value + "分钟";
    }

    private int parseLeadingInt(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private Long parseParentId(Object level) {
        int value = asInteger(level, 1);
        return value <= 1 ? 0L : 1L;
    }

    private boolean parseBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return fallback;
        }
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text);
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
