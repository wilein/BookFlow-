package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Comment;
import com.book.bookflow.entity.ContentReport;
import com.book.bookflow.entity.LearningPath;
import com.book.bookflow.entity.Notification;
import com.book.bookflow.entity.PathNode;
import com.book.bookflow.entity.Post;
import com.book.bookflow.entity.PostAction;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserProfile;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.CommentMapper;
import com.book.bookflow.mapper.ContentReportMapper;
import com.book.bookflow.mapper.LearningPathMapper;
import com.book.bookflow.mapper.NotificationMapper;
import com.book.bookflow.mapper.PathNodeMapper;
import com.book.bookflow.mapper.PostActionMapper;
import com.book.bookflow.mapper.PostMapper;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.mapper.UserProfileMapper;
import com.book.bookflow.service.CommunityService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostActionMapper postActionMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final LearningPathMapper learningPathMapper;
    private final PathNodeMapper pathNodeMapper;
    private final ContentReportMapper contentReportMapper;
    private final NotificationMapper notificationMapper;

    public CommunityServiceImpl(PostMapper postMapper,
                                CommentMapper commentMapper,
                                PostActionMapper postActionMapper,
                                UserMapper userMapper,
                                UserProfileMapper userProfileMapper,
                                LearningPathMapper learningPathMapper,
                                PathNodeMapper pathNodeMapper,
                                ContentReportMapper contentReportMapper,
                                NotificationMapper notificationMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.postActionMapper = postActionMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.learningPathMapper = learningPathMapper;
        this.pathNodeMapper = pathNodeMapper;
        this.contentReportMapper = contentReportMapper;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public List<Map<String, Object>> getFeed(String type) {
        List<Post> posts = postMapper.selectListByQuery(
            QueryWrapper.create().where("is_deleted = 0").orderBy("id desc").limit(40)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> item = toPostItem(post);
            if (type == null || type.isBlank() || "recommend".equals(type) || type.equals(item.get("type"))) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getPostDetail(Long postId) {
        Post post = requirePost(postId);
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        post.setUpdateTime(LocalDateTime.now());
        postMapper.update(post);
        Map<String, Object> item = toPostItem(post);
        item.put("views", post.getViewCount() == null ? 0 : post.getViewCount());
        return item;
    }

    @Override
    public List<Map<String, Object>> getActivity() {
        List<Post> posts = postMapper.selectListByQuery(
            QueryWrapper.create().where("is_deleted = 0").orderBy("id desc").limit(8)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "post-" + post.getId());
            item.put("title", "社区内容更新");
            item.put("desc", defaultString(post.getTitle(), "你关注的内容有更新"));
            item.put("time", formatRelative(post.getCreateTime()));
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> createPost(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        String title = defaultString(payload.get("title"), "");
        String content = defaultString(payload.get("content"), "");
        String type = defaultString(payload.get("type"), "recommend");
        Long sharedPathId = asLong(payload.get("sharedPathId"));
        if (title.isBlank()) {
            throw new CustomerException("400", "请填写帖子标题");
        }
        if (content.isBlank()) {
            throw new CustomerException("400", "请填写帖子内容");
        }
        if ("path".equals(type) && sharedPathId != null) {
            LearningPath path = learningPathMapper.selectOneById(sharedPathId);
            if (path == null || isDeleted(path.getIsDeleted()) || !currentUserId.equals(path.getUserId())) {
                throw new CustomerException("403", "只能分享自己的学习路径");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder()
            .userId(currentUserId)
            .title(title)
            .content(content)
            .type(mapPostTypeValue(type))
            .sharedPathId(sharedPathId)
            .viewCount(0)
            .likeCount(0)
            .commentCount(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        postMapper.insert(post);
        return toPostItem(post);
    }

    @Override
    public Map<String, Object> togglePostLike(Long postId) {
        return togglePostAction(postId, 1);
    }

    @Override
    public Map<String, Object> togglePostFavorite(Long postId) {
        return togglePostAction(postId, 2);
    }

    @Override
    public List<Map<String, Object>> getCommentList(Long postId) {
        if (postId == null) {
            throw new CustomerException("400", "帖子参数错误");
        }
        List<Comment> comments = commentMapper.selectListByQuery(
            QueryWrapper.create().where("post_id = ?", postId).and("is_deleted = 0").orderBy("id desc").limit(100)
        );
        Map<Long, User> usersById = loadUsersByIds(
            comments.stream().map(Comment::getUserId).filter(Objects::nonNull).distinct().toList()
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment comment : comments) {
            User user = comment.getUserId() == null ? null : usersById.get(comment.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", comment.getId());
            item.put("postId", comment.getPostId());
            item.put("userId", comment.getUserId());
            item.put("nickname", user != null ? defaultString(user.getNickname(), "书友") : "书友");
            item.put("avatar", user != null ? defaultString(user.getAvatarUrl(), "") : "");
            item.put("content", defaultString(comment.getContent(), ""));
            item.put("likeCount", comment.getLikeCount() == null ? 0 : comment.getLikeCount());
            item.put("createTime", formatRelative(comment.getCreateTime()));
            returnListItemMeta(item, user);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> createComment(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long postId = asLong(payload.get("postId"));
        String content = defaultString(payload.get("content"), "");
        if (postId == null) {
            throw new CustomerException("400", "帖子参数错误");
        }
        if (content.isBlank()) {
            throw new CustomerException("400", "请输入评论内容");
        }
        Post post = requirePost(postId);

        LocalDateTime now = LocalDateTime.now();
        Comment comment = Comment.builder()
            .postId(postId)
            .userId(currentUserId)
            .content(content)
            .likeCount(0)
            .createTime(now)
            .isDeleted(0)
            .build();
        commentMapper.insert(comment);

        long commentCount = commentMapper.selectCountByQuery(
            QueryWrapper.create().where("post_id = ?", postId).and("is_deleted = 0")
        );
        post.setCommentCount((int) commentCount);
        post.setUpdateTime(now);
        postMapper.update(post);
        if (!currentUserId.equals(post.getUserId())) {
            createNotification(post.getUserId(), "community", "你的帖子收到新评论", defaultString(post.getTitle(), "社区帖子"), "/pages/community/comments?postId=" + postId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", comment.getId());
        result.put("postId", postId);
        result.put("commentCount", commentCount);
        return result;
    }

    @Override
    public Map<String, Object> reportPost(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        Long postId = asLong(payload.get("postId"));
        String reasonType = defaultString(payload.get("reasonType"), "其他");
        String content = defaultString(payload.get("content"), "");
        Post post = requirePost(postId);
        if (content.isBlank()) {
            throw new CustomerException("400", "请填写举报说明");
        }
        LocalDateTime now = LocalDateTime.now();
        ContentReport report = ContentReport.builder()
            .userId(currentUserId)
            .targetType("post")
            .targetId(postId)
            .reasonType(reasonType)
            .content(content)
            .status(0)
            .createTime(now)
            .updateTime(now)
            .isDeleted(0)
            .build();
        contentReportMapper.insert(report);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", report.getId());
        result.put("postId", postId);
        result.put("status", 0);
        result.put("postTitle", defaultString(post.getTitle(), ""));
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyReports() {
        Long currentUserId = AuthContext.requireUserId();
        List<ContentReport> reports = contentReportMapper.selectListByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("target_type = 'post'")
                .and("is_deleted = 0")
                .orderBy("id desc")
                .limit(100)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (ContentReport report : reports) {
            Post post = report.getTargetId() == null ? null : postMapper.selectOneById(report.getTargetId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", report.getId());
            item.put("targetId", report.getTargetId());
            item.put("targetType", defaultString(report.getTargetType(), ""));
            item.put("reasonType", defaultString(report.getReasonType(), ""));
            item.put("content", defaultString(report.getContent(), ""));
            item.put("status", report.getStatus());
            item.put("statusLabel", mapReportStatus(report.getStatus()));
            item.put("postTitle", post != null ? defaultString(post.getTitle(), "社区帖子") : "社区帖子");
            item.put("createTime", formatRelative(report.getCreateTime()));
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> togglePostAction(Long postId, int actionType) {
        Long currentUserId = AuthContext.requireUserId();
        Post post = requirePost(postId);
        PostAction action = postActionMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("post_id = ?", postId)
                .and("action_type = ?", actionType)
                .limit(1)
        );

        boolean active;
        LocalDateTime now = LocalDateTime.now();
        if (action == null) {
            action = PostAction.builder()
                .userId(currentUserId)
                .postId(postId)
                .actionType(actionType)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            postActionMapper.insert(action);
            active = true;
        } else if (isDeleted(action.getIsDeleted())) {
            action.setIsDeleted(0);
            action.setUpdateTime(now);
            postActionMapper.update(action);
            active = true;
        } else {
            action.setIsDeleted(1);
            action.setUpdateTime(now);
            postActionMapper.update(action);
            active = false;
        }

        long likeCount = countPostAction(postId, 1);
        long favoriteCount = countPostAction(postId, 2);
        post.setLikeCount((int) likeCount);
        post.setUpdateTime(now);
        postMapper.update(post);
        if (active && !currentUserId.equals(post.getUserId())) {
            createNotification(
                post.getUserId(),
                "community",
                actionType == 1 ? "你的帖子收到新点赞" : "你的帖子被收藏",
                defaultString(post.getTitle(), "社区帖子"),
                "/pages/community/comments?postId=" + postId
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", actionType == 1 && active);
        result.put("favorited", actionType == 2 && active);
        result.put("likeCount", likeCount);
        result.put("favoriteCount", favoriteCount);
        return result;
    }

    private long countPostAction(Long postId, int actionType) {
        return postActionMapper.selectCountByQuery(
            QueryWrapper.create()
                .where("post_id = ?", postId)
                .and("action_type = ?", actionType)
                .and("is_deleted = 0")
        );
    }

    private Map<String, Object> toPostItem(Post post) {
        Long currentUserId = AuthContext.getUserId();
        User user = post.getUserId() == null ? null : userMapper.selectOneById(post.getUserId());
        UserProfile profile = post.getUserId() == null ? null : userProfileMapper.selectOneByQuery(
            QueryWrapper.create().where("user_id = ?", post.getUserId()).and("is_deleted = 0").limit(1)
        );

        String postType = mapPostType(post.getType());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", post.getId());
        item.put("type", postType);
        item.put("author", user != null ? defaultString(user.getNickname(), "校园同学") : "校园同学");
        item.put("school", buildSchool(profile));
        item.put("time", formatRelative(post.getCreateTime()));
        item.put("title", defaultString(post.getTitle(), "未命名帖子"));
        item.put("content", defaultString(post.getContent(), ""));
        item.put("tags", buildTags(postType));
        item.put("likes", post.getLikeCount() == null ? 0 : post.getLikeCount());
        item.put("comments", post.getCommentCount() == null ? 0 : post.getCommentCount());
        item.put("favoriteCount", countPostAction(post.getId(), 2));
        item.put("liked", currentUserId != null && hasPostAction(currentUserId, post.getId(), 1));
        item.put("favorited", currentUserId != null && hasPostAction(currentUserId, post.getId(), 2));
        item.put("avatar", user != null ? defaultString(user.getAvatarUrl(), "") : "");
        if (Objects.equals(post.getType(), 3) && post.getSharedPathId() != null) {
            item.put("sharedPath", toSharedPathCard(post.getSharedPathId()));
        }
        return item;
    }

    private Map<String, Object> toSharedPathCard(Long pathId) {
        LearningPath path = learningPathMapper.selectOneById(pathId);
        if (path == null || isDeleted(path.getIsDeleted())) {
            return null;
        }
        long nodeCount = pathNodeMapper.selectCountByQuery(
            QueryWrapper.create().where("path_id = ?", pathId).and("is_deleted = 0")
        );
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", path.getId());
        card.put("title", defaultString(path.getTitle(), "学习路径"));
        card.put("description", defaultString(path.getDescription(), ""));
        card.put("coverImage", defaultString(path.getCoverImage(), ""));
        card.put("cover", defaultString(path.getCoverImage(), ""));
        card.put("difficulty", mapDifficulty(path.getDifficulty()));
        card.put("totalDuration", (path.getEstimatedHours() == null ? 0 : path.getEstimatedHours()) + "小时");
        card.put("nodeCount", nodeCount);
        card.put("sourcePathId", path.getSourcePathId());
        return card;
    }

    private boolean hasPostAction(Long userId, Long postId, int actionType) {
        return postActionMapper.selectCountByQuery(
            QueryWrapper.create()
                .where("user_id = ?", userId)
                .and("post_id = ?", postId)
                .and("action_type = ?", actionType)
                .and("is_deleted = 0")
        ) > 0;
    }

    private Map<Long, User> loadUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        List<User> users = userMapper.selectListByQuery(
            QueryWrapper.create().where("id in (" + placeholders + ")", ids.toArray())
        );
        Map<Long, User> result = new LinkedHashMap<>();
        for (User user : users) {
            result.put(user.getId(), user);
        }
        return result;
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

    private Post requirePost(Long postId) {
        if (postId == null) {
            throw new CustomerException("400", "帖子参数错误");
        }
        Post post = postMapper.selectOneById(postId);
        if (post == null || isDeleted(post.getIsDeleted())) {
            throw new CustomerException("404", "帖子不存在");
        }
        return post;
    }

    private String mapPostType(Integer type) {
        int value = type == null ? 0 : type;
        return switch (value) {
            case 1 -> "review";
            case 2 -> "qa";
            case 3 -> "path";
            default -> "recommend";
        };
    }

    private int mapPostTypeValue(String type) {
        return switch (type) {
            case "review" -> 1;
            case "qa" -> 2;
            case "path" -> 3;
            default -> 0;
        };
    }

    private List<String> buildTags(String type) {
        return switch (type) {
            case "review" -> List.of("书评", "阅读笔记");
            case "qa" -> List.of("提问", "学习交流");
            case "path" -> List.of("学习路径", "经验整理");
            default -> List.of("推荐", "校园交流");
        };
    }

    private String buildSchool(UserProfile profile) {
        if (profile == null) {
            return "校园用户";
        }
        String school = defaultString(profile.getSchool(), "");
        String department = defaultString(profile.getDepartment(), "");
        if (!school.isBlank() && !department.isBlank()) {
            return school + " / " + department;
        }
        return school.isBlank() ? "校园用户" : school;
    }

    private String mapDifficulty(Integer difficulty) {
        int value = difficulty == null ? 1 : difficulty;
        return switch (value) {
            case 2 -> "中级";
            case 3 -> "进阶";
            default -> "入门";
        };
    }

    private String mapReportStatus(Integer status) {
        int value = status == null ? 0 : status;
        return switch (value) {
            case 1 -> "已查看";
            case 2 -> "已关闭";
            default -> "待处理";
        };
    }

    private String formatRelative(LocalDateTime time) {
        if (time == null) {
            return "刚刚";
        }
        Duration duration = Duration.between(time, LocalDateTime.now());
        long minutes = Math.max(1, duration.toMinutes());
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "小时前";
        }
        return duration.toDays() + "天前";
    }

    private void returnListItemMeta(Map<String, Object> item, User user) {
        item.put("displayName", user != null ? defaultString(user.getNickname(), "书友") : "书友");
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
