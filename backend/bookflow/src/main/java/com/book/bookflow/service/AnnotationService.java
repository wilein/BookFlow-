package com.book.bookflow.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface AnnotationService {
    Map<String, Object> getAnnotationList(Long bookId);

    Map<String, Object> getAnnotationList(Long bookId, boolean mineOnly);

    Map<String, Object> createAnnotation(Map<String, Object> payload);

    Map<String, Object> uploadAnnotationImage(MultipartFile file);

    Map<String, Object> toggleLike(Long annotationId);
}
