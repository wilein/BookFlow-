package com.book.bookflow.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.book.bookflow.common.config.WxSessionRedisService;
import com.book.bookflow.common.utils.JwtUtil;
import com.book.bookflow.common.utils.WxDecryptUtil;
import com.book.bookflow.entity.User;
import com.book.bookflow.entity.UserProfile;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.mapper.UserMapper;
import com.book.bookflow.mapper.UserProfileMapper;
import com.book.bookflow.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private static final long TOKEN_EXPIRE_MILLIS = 15L * 24 * 60 * 60 * 1000;
    private static final long TOKEN_EXPIRE_SECONDS = 15L * 24 * 60 * 60;
    private static final String DEFAULT_NICKNAME = "书友";
    private static final String DEFAULT_INTRO = "个人信息待完善";

    @Value("${weixin.appid}")
    private String appid;

    @Value("${weixin.secret}")
    private String secret;

    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final WxSessionRedisService wxSessionRedisService;

    public UserServiceImpl(
        RestTemplate restTemplate,
        UserMapper userMapper,
        UserProfileMapper userProfileMapper,
        WxSessionRedisService wxSessionRedisService
    ) {
        this.restTemplate = restTemplate;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.wxSessionRedisService = wxSessionRedisService;
    }

    @Override
    public Map<String, Object> loginWithWechat(String code) {
        // 微信登录
        if (code == null || code.isBlank()) {
            throw new CustomerException("400", "缺少登录凭证");
        }
        Map<String, String> wxResult = getOpenidByCode(code);
        String openid = wxResult.get("openid");
        String sessionKey = wxResult.get("session_key");
        String unionid = wxResult.get("unionid");
        if (openid == null || openid.isBlank() || sessionKey == null || sessionKey.isBlank()) {
            throw new CustomerException("500", "微信登录失败");
        }

        User user = userMapper.selectOneByQuery(
            QueryWrapper.create().where("openid = ?", openid).and("is_deleted = 0").limit(1)
        );
        boolean isNewUser = false;
        if (user == null) {
            user = registerNewUser(openid, unionid, sessionKey);
            isNewUser = true;
        } else {
            updateUserSession(user, sessionKey);
        }

        UserProfile profile = getOrCreateProfile(user.getId(), true);
        String token = issueFreshToken(null, user);
        wxSessionRedisService.saveSessionKey(openid, sessionKey);
        return buildSessionResult(user, profile, token, isNewUser);
    }

    @Override
    public Map<String, Object> loginForDev() {
        String openid = "dev-openid-10001";
        String sessionKey = "dev-session-key";
        User user = userMapper.selectOneByQuery(
            QueryWrapper.create().where("openid = ?", openid).and("is_deleted = 0").limit(1)
        );
        boolean isNewUser = false;
        if (user == null) {
            user = registerNewUser(openid, "dev-unionid-10001", sessionKey);
            isNewUser = true;
        } else {
            updateUserSession(user, sessionKey);
        }

        UserProfile profile = getOrCreateProfile(user.getId(), true);
        String token = issueFreshToken(null, user);
        wxSessionRedisService.saveSessionKey(openid, sessionKey);
        return buildSessionResult(user, profile, token, isNewUser);
    }

    @Override
    public void updateUserInfo(String encryptedData, String iv, String openid) {
        try {
            String sessionKey = wxSessionRedisService.getSessionKey(openid);
            if (sessionKey == null || sessionKey.isBlank()) {
                throw new CustomerException("401", "登录状态已过期");
            }
            String decryptedData = WxDecryptUtil.decrypt(encryptedData, sessionKey, iv);
            JSONObject userData = JSONObject.parseObject(decryptedData);
            if (!validateWatermark(userData)) {
                throw new CustomerException("500", "数据校验失败");
            }
            updateUserProfile(openid, userData);
            wxSessionRedisService.extendSessionKey(openid);
        } catch (CustomerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomerException("500", "更新用户信息失败");
        }
    }

    @Override
    public String getPhoneNumber(String encryptedData, String iv, String openid) {
        try {
            String sessionKey = wxSessionRedisService.getSessionKey(openid);
            if (sessionKey == null || sessionKey.isBlank()) {
                throw new CustomerException("401", "登录状态已过期");
            }
            String decryptedData = WxDecryptUtil.decrypt(encryptedData, sessionKey, iv);
            JSONObject phoneInfo = JSONObject.parseObject(decryptedData);
            if (!validateWatermark(phoneInfo)) {
                throw new CustomerException("500", "手机号数据校验失败");
            }
            String phoneNumber = phoneInfo.getString("purePhoneNumber");
            updateUserPhone(openid, phoneNumber);
            wxSessionRedisService.extendSessionKey(openid);
            return phoneNumber;
        } catch (CustomerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomerException("500", "获取手机号失败");
        }
    }

    @Override
    public boolean verifyToken(String token) {
        String cleanToken = normalizeToken(token);
        if (cleanToken.isBlank()) {
            return false;
        }
        if (!JwtUtil.verifyToken(cleanToken)) {
            return false;
        }
        Long userId = JwtUtil.getUserId(cleanToken);
        if (userId == null) {
            return false;
        }
        Long redisUserId = wxSessionRedisService.getUserIdByToken(cleanToken);
        return redisUserId != null && redisUserId.equals(userId);
    }

    @Override
    public void logout(String token) {
        String cleanToken = normalizeToken(token);
        if (cleanToken.isBlank()) {
            return;
        }
        String openid = JwtUtil.getOpenid(cleanToken);
        wxSessionRedisService.deleteUserToken(cleanToken);
        if (openid != null && !openid.isBlank()) {
            wxSessionRedisService.deleteSessionKey(openid);
        }
    }

    @Override
    public Map<String, Object> getUserInfoByToken(String token) {
        String cleanToken = normalizeToken(token);
        if (!verifyToken(cleanToken)) {
            throw new CustomerException("401", "登录状态已过期");
        }
        User user = getUserById(JwtUtil.getUserId(cleanToken));
        UserProfile profile = getOrCreateProfile(user.getId(), true);
        return buildUserInfo(user, profile);
    }

    @Override
    public Map<String, Object> checkLoginStatus(String token) {
        String cleanToken = normalizeToken(token);
        if (!verifyToken(cleanToken)) {
            throw new CustomerException("401", "登录状态已过期");
        }
        User user = getUserById(JwtUtil.getUserId(cleanToken));
        UserProfile profile = getOrCreateProfile(user.getId(), true);
        String newToken = issueFreshToken(cleanToken, user);
        return buildSessionResult(user, profile, newToken, false);
    }

    private Map<String, String> getOpenidByCode(String code) {
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appid,
            secret,
            code
        );
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject result = JSONObject.parseObject(response.getBody());
            if (result == null) {
                throw new CustomerException("500", "微信登录失败");
            }
            if (result.containsKey("errcode")) {
                throw new CustomerException("500", result.getString("errmsg"));
            }
            Map<String, String> wxResult = new LinkedHashMap<>();
            wxResult.put("openid", result.getString("openid"));
            wxResult.put("session_key", result.getString("session_key"));
            wxResult.put("unionid", result.getString("unionid"));
            return wxResult;
        } catch (CustomerException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Fetch openid failed", exception);
            throw new CustomerException("500", "微信登录失败");
        }
    }

    private User registerNewUser(String openid, String unionid, String sessionKey) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setOpenid(openid);
        user.setUnionid(unionid);
        user.setSessionKey(sessionKey);
        user.setNickname(DEFAULT_NICKNAME);
        user.setAvatarUrl("");
        user.setMobile("");
        user.setProvince("");
        user.setCity("");
        user.setLastLoginTime(now);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setIsDeleted(0);
        userMapper.insert(user);
        getOrCreateProfile(user.getId(), true);
        return user;
    }

    private void updateUserSession(User user, String sessionKey) {
        user.setSessionKey(sessionKey);
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(DEFAULT_NICKNAME);
        }
        userMapper.update(user);
        getOrCreateProfile(user.getId(), true);
    }

    private UserProfile getOrCreateProfile(Long userId, boolean createWhenMissing) {
        UserProfile profile = userProfileMapper.selectOneByQuery(
            QueryWrapper.create().where("user_id = ?", userId).and("is_deleted = 0").limit(1)
        );
        if (profile == null && createWhenMissing) {
            LocalDateTime now = LocalDateTime.now();
            profile = UserProfile.builder()
                .userId(userId)
                .authStatus(0)
                .creditScore(88)
                .intro(DEFAULT_INTRO)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
            userProfileMapper.insert(profile);
        }
        return profile;
    }

    private Map<String, Object> buildSessionResult(User user, UserProfile profile, String token, boolean isNewUser) {
        long expireAt = System.currentTimeMillis() + TOKEN_EXPIRE_MILLIS;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("openid", user.getOpenid());
        result.put("expiresIn", TOKEN_EXPIRE_SECONDS);
        result.put("expireAt", expireAt);
        result.put("isNewUser", isNewUser);
        result.put("userInfo", buildUserInfo(user, profile));
        return result;
    }

    private Map<String, Object> buildUserInfo(User user, UserProfile profile) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        boolean profileIncomplete = isProfileIncomplete(user, profile);
        int authStatus = profile != null && profile.getAuthStatus() != null ? profile.getAuthStatus() : 0;
        userInfo.put("userId", user.getId());
        userInfo.put("openid", user.getOpenid());
        userInfo.put("nickname", defaultString(user.getNickname(), DEFAULT_NICKNAME));
        userInfo.put("displayName", defaultString(user.getNickname(), DEFAULT_NICKNAME));
        userInfo.put("avatarUrl", defaultString(user.getAvatarUrl(), ""));
        userInfo.put("avatar", defaultString(user.getAvatarUrl(), ""));
        userInfo.put("mobile", defaultString(user.getMobile(), ""));
        userInfo.put("city", defaultString(user.getCity(), ""));
        userInfo.put("province", defaultString(user.getProvince(), ""));
        userInfo.put("profileIncomplete", profileIncomplete);
        userInfo.put("verified", authStatus == 2);
        userInfo.put("authStatus", authStatus);
        userInfo.put("signature", buildSignature(profile, profileIncomplete));
        return userInfo;
    }

    private boolean isProfileIncomplete(User user, UserProfile profile) {
        String nickname = defaultString(user.getNickname(), DEFAULT_NICKNAME);
        String school = profile == null ? "" : defaultString(profile.getSchool(), "");
        String department = profile == null ? "" : defaultString(profile.getDepartment(), "");
        String intro = profile == null ? "" : defaultString(profile.getIntro(), "");
        return DEFAULT_NICKNAME.equals(nickname) || (school.isBlank() && department.isBlank() && (intro.isBlank() || DEFAULT_INTRO.equals(intro)));
    }

    private String buildSignature(UserProfile profile, boolean profileIncomplete) {
        if (profile == null || profileIncomplete) {
            return DEFAULT_INTRO;
        }
        String school = defaultString(profile.getSchool(), "");
        String department = defaultString(profile.getDepartment(), "");
        if (!school.isBlank() && !department.isBlank()) {
            return school + " · " + department;
        }
        if (!school.isBlank()) {
            return school;
        }
        return DEFAULT_INTRO;
    }

    private String issueFreshToken(String oldToken, User user) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("userId", user.getId());
        claims.put("openid", user.getOpenid());
        claims.put("timestamp", System.currentTimeMillis());
        String token = JwtUtil.genToken(claims, TOKEN_EXPIRE_MILLIS);
        // Do not revoke the previous token immediately.
        // Frontend pages often fire several requests in parallel after login or page enter.
        // If the first request refreshes the token and the old one is deleted at once,
        // the remaining in-flight requests will be treated as expired.
        if (oldToken != null && !oldToken.isBlank()) {
            wxSessionRedisService.extendUserToken(oldToken);
        }
        wxSessionRedisService.saveUserToken(token, user.getId());
        wxSessionRedisService.extendSessionKey(user.getOpenid());
        return token;
    }

    private boolean validateWatermark(JSONObject userData) {
        try {
            JSONObject watermark = userData.getJSONObject("watermark");
            if (watermark == null) {
                return false;
            }
            String appidInData = watermark.getString("appid");
            long timestamp = watermark.getLongValue("timestamp");
            if (!appid.equals(appidInData)) {
                return false;
            }
            long currentTime = System.currentTimeMillis() / 1000;
            return Math.abs(currentTime - timestamp) <= 300;
        } catch (Exception exception) {
            return false;
        }
    }

    private void updateUserProfile(String openid, JSONObject userData) {
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where("openid = ?", openid).limit(1));
        if (user == null) {
            return;
        }
        user.setNickname(defaultString(userData.getString("nickName"), DEFAULT_NICKNAME));
        user.setAvatarUrl(defaultString(userData.getString("avatarUrl"), ""));
        user.setGender(userData.getInteger("gender"));
        user.setCountry(defaultString(userData.getString("country"), ""));
        user.setProvince(defaultString(userData.getString("province"), ""));
        user.setCity(defaultString(userData.getString("city"), ""));
        if (userData.containsKey("unionId")) {
            user.setUnionid(defaultString(userData.getString("unionId"), user.getUnionid()));
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    private void updateUserPhone(String openid, String phoneNumber) {
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where("openid = ?", openid).limit(1));
        if (user == null) {
            return;
        }
        user.setMobile(defaultString(phoneNumber, ""));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    private User getUserById(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new CustomerException("401", "用户不存在");
        }
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(DEFAULT_NICKNAME);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.update(user);
        }
        return user;
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        String cleanToken = token.trim();
        if (cleanToken.startsWith("Bearer ")) {
            return cleanToken.substring(7).trim();
        }
        return cleanToken;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
