package com.book.bookflow.service;

import java.util.Map;

public interface FavoriteService {
    Map<String, Object> toggleFavorite(Map<String, Object> payload);

    Map<String, Object> getFavoriteStatus(String targetType, Long targetId);
}
