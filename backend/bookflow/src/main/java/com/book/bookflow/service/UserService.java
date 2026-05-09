package com.book.bookflow.service;

import java.util.Map;

public interface UserService {

    Map<String, Object> loginWithWechat(String code);

    Map<String, Object> loginForDev();

    void updateUserInfo(String encryptedData, String iv, String openid);

    String getPhoneNumber(String encryptedData, String iv, String openid);

    boolean verifyToken(String token);

    void logout(String token);

    Map<String, Object> getUserInfoByToken(String token);

    Map<String, Object> checkLoginStatus(String token);
}
