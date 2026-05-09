package com.book.bookflow.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BookService {
    List<Map<String, Object>> getBooksList();

    Map<String, List<Map<String, Object>>> getBooksByCategory();

    Map<String, Object> searchBooks(String keyword, String category, Integer pageNo, Integer pageSize);

    Map<String, Object> getBookDetail(Long bookId);

    Map<String, Object> publishBook(Map<String, Object> payload);

    Map<String, Object> uploadBookImage(MultipartFile file);

    Map<String, Object> updateBook(Map<String, Object> payload);

    Map<String, Object> changeBookStatus(Map<String, Object> payload);
}
