package com.book.bookflow.service;

import java.util.List;
import java.util.Map;

public interface CommunityService {
    List<Map<String, Object>> getFeed(String type);

    Map<String, Object> getPostDetail(Long postId);

    List<Map<String, Object>> getActivity();

    Map<String, Object> createPost(Map<String, Object> payload);

    Map<String, Object> togglePostLike(Long postId);

    Map<String, Object> togglePostFavorite(Long postId);

    List<Map<String, Object>> getCommentList(Long postId);

    Map<String, Object> createComment(Map<String, Object> payload);

    Map<String, Object> reportPost(Map<String, Object> payload);

    List<Map<String, Object>> getMyReports();
}
