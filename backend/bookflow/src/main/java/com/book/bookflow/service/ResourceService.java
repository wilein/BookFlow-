package com.book.bookflow.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResourceService {
    List<Map<String, Object>> getMyResources();

    Map<String, Object> getResourceDetail(Long id);

    List<Map<String, Object>> getResources(Long bookId, Long pathNodeId);

    Map<String, Object> uploadResourceFile(MultipartFile file);

    Map<String, Object> createResource(Map<String, Object> payload);

    Map<String, Object> updateResource(Map<String, Object> payload);

    Map<String, Object> deleteResource(Map<String, Object> payload);
}
