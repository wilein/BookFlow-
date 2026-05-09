package com.book.bookflow.service.impl;

import com.book.bookflow.common.auth.AuthContext;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.LearningPath;
import com.book.bookflow.entity.UserFavorite;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.BookMapper;
import com.book.bookflow.mapper.LearningPathMapper;
import com.book.bookflow.mapper.UserFavoriteMapper;
import com.book.bookflow.service.FavoriteService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final BookMapper bookMapper;
    private final LearningPathMapper learningPathMapper;

    public FavoriteServiceImpl(UserFavoriteMapper userFavoriteMapper,
                               BookMapper bookMapper,
                               LearningPathMapper learningPathMapper) {
        this.userFavoriteMapper = userFavoriteMapper;
        this.bookMapper = bookMapper;
        this.learningPathMapper = learningPathMapper;
    }

    @Override
    public Map<String, Object> toggleFavorite(Map<String, Object> payload) {
        Long currentUserId = AuthContext.requireUserId();
        String targetType = normalizeTargetType(payload.get("targetType"));
        Long targetId = asLong(payload.get("targetId"));
        int targetTypeValue = mapTargetTypeValue(targetType);
        validateTarget(targetType, targetId);

        UserFavorite favorite = userFavoriteMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("target_type = ?", targetTypeValue)
                .and("target_id = ?", targetId)
                .limit(1)
        );

        boolean favorited;
        if (favorite == null) {
            favorite = UserFavorite.builder()
                .userId(currentUserId)
                .targetType(targetTypeValue)
                .targetId(targetId)
                .createTime(LocalDateTime.now())
                .build();
            userFavoriteMapper.insert(favorite);
            favorited = true;
        } else {
            userFavoriteMapper.deleteById(favorite.getId());
            favorited = false;
        }

        long favoriteCount = countFavorites(targetTypeValue, targetId);
        syncFavoriteCount(targetType, targetId, favoriteCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favorited", favorited);
        result.put("favoriteCount", favoriteCount);
        return result;
    }

    @Override
    public Map<String, Object> getFavoriteStatus(String targetType, Long targetId) {
        Long currentUserId = AuthContext.requireUserId();
        String normalizedType = normalizeTargetType(targetType);
        int targetTypeValue = mapTargetTypeValue(normalizedType);
        validateTarget(normalizedType, targetId);

        UserFavorite favorite = userFavoriteMapper.selectOneByQuery(
            QueryWrapper.create()
                .where("user_id = ?", currentUserId)
                .and("target_type = ?", targetTypeValue)
                .and("target_id = ?", targetId)
                .limit(1)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favorited", favorite != null);
        result.put("favoriteCount", countFavorites(targetTypeValue, targetId));
        return result;
    }

    private void validateTarget(String targetType, Long targetId) {
        if (targetId == null) {
            throw new CustomerException("400", "收藏参数错误");
        }
        if ("book".equals(targetType)) {
            Book book = bookMapper.selectOneById(targetId);
            if (book == null || isDeleted(book.getIsDeleted())) {
                throw new CustomerException("404", "书籍不存在");
            }
            return;
        }
        LearningPath path = learningPathMapper.selectOneById(targetId);
        if (path == null || isDeleted(path.getIsDeleted())) {
            throw new CustomerException("404", "路径不存在");
        }
    }

    private long countFavorites(int targetType, Long targetId) {
        return userFavoriteMapper.selectCountByQuery(
            QueryWrapper.create()
                .where("target_type = ?", targetType)
                .and("target_id = ?", targetId)
        );
    }

    private void syncFavoriteCount(String targetType, Long targetId, long favoriteCount) {
        if ("book".equals(targetType)) {
            Book book = bookMapper.selectOneById(targetId);
            if (book != null && !isDeleted(book.getIsDeleted())) {
                book.setFavoriteCount((int) favoriteCount);
                book.setUpdateTime(LocalDateTime.now());
                bookMapper.update(book);
            }
            return;
        }
        LearningPath path = learningPathMapper.selectOneById(targetId);
        if (path != null && !isDeleted(path.getIsDeleted())) {
            path.setFavoriteCount((int) favoriteCount);
            path.setUpdateTime(LocalDateTime.now());
            learningPathMapper.update(path);
        }
    }

    private String normalizeTargetType(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim().toLowerCase();
        return "path".equals(text) ? "path" : "book";
    }

    private int mapTargetTypeValue(String targetType) {
        return "path".equals(targetType) ? 2 : 1;
    }

    private boolean isDeleted(Integer isDeleted) {
        return isDeleted != null && isDeleted == 1;
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
