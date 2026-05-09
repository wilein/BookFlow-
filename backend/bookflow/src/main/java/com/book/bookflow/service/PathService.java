package com.book.bookflow.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface PathService {
    List<Map<String, Object>> getPublicPaths(String category, String keyword);

    Map<String, Object> getPathDetail(Long pathId);

    Map<String, Object> saveDraft(Map<String, Object> payload);

    Map<String, Object> publishPath(Map<String, Object> payload);

    Map<String, Object> uploadPathCover(MultipartFile file);

    Map<String, Object> startLearning(Long pathId);

    Map<String, Object> cancelLearning(Long pathId);

    List<Map<String, Object>> getMyLearningPaths();

    Map<String, Object> getCurrentLearningPath();

    Map<String, Object> completeNode(Map<String, Object> payload);

    Map<String, Object> copyPath(Long pathId);

    Map<String, Object> changePathStatus(Map<String, Object> payload);
}
