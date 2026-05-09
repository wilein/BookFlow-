package com.book.bookflow.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ProfileService {
    Map<String, Object> getProfile();

    Map<String, Object> getUserStats();

    Map<String, Object> updateProfile(Map<String, Object> payload);

    Map<String, Object> submitStudentVerify(Map<String, Object> payload);

    Map<String, Object> uploadProfileImage(MultipartFile file, String type);

    Path resolveOwnStudentCardImagePath(String rawUrl);

    List<Map<String, Object>> getMyBookshelf(String status);

    List<Map<String, Object>> getMyOrders(String status, String role);

    List<Map<String, Object>> getMyFavorites(String type);

    List<Map<String, Object>> getMyAnnotations();

    List<Map<String, Object>> getMyPaths();

    List<Map<String, Object>> getAddressList();

    Map<String, Object> saveAddress(Map<String, Object> payload);

    Map<String, Object> deleteAddress(Map<String, Object> payload);

    Map<String, Object> setDefaultAddress(Map<String, Object> payload);

    List<Map<String, Object>> getBrowseHistory();

    Map<String, Object> recordBrowseHistory(Map<String, Object> payload);

    Map<String, Object> deleteBrowseHistory(Map<String, Object> payload);

    Map<String, Object> clearBrowseHistory();

    Map<String, Object> submitFeedback(Map<String, Object> payload);

    List<Map<String, Object>> getNotifications();

    Map<String, Object> readNotification(Map<String, Object> payload);

}
