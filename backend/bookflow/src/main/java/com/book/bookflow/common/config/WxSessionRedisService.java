package com.book.bookflow.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class WxSessionRedisService {

    private final RedisService redisService;

    // 微信session_key前缀
    private static final String WX_SESSION_PREFIX = "wx_session:";
    // 微信access_token前缀（如果需要）
    private static final String WX_TOKEN_PREFIX = "wx_token:";
    // 用户token前缀
    private static final String USER_TOKEN_PREFIX = "user_token:";

    // 过期时间（秒）
    private static final long SESSION_EXPIRE = 7200; // 2小时
    private static final long TOKEN_EXPIRE = 7 * 24 * 3600; // 7天
    private static final long ACCESS_TOKEN_EXPIRE = 7100; // 微信access_token 1小时50分钟

    public WxSessionRedisService(RedisService redisService) {
        this.redisService = redisService;
    }

    // ==================== Session Key 相关 ====================

    /**
     * 保存微信session_key
     * @param openid 用户openid
     * @param sessionKey 微信session_key
     */
    public void saveSessionKey(String openid, String sessionKey) {
        String key = WX_SESSION_PREFIX + openid;
        redisService.set(key, sessionKey, SESSION_EXPIRE, TimeUnit.SECONDS);
        log.debug("保存session_key, openid: {}, expire: {}秒", openid, SESSION_EXPIRE);
    }

    /**
     * 获取微信session_key
     * @param openid 用户openid
     * @return session_key
     */
    public String getSessionKey(String openid) {
        String key = WX_SESSION_PREFIX + openid;
        return redisService.getString(key);
    }

    /**
     * 删除微信session_key
     * @param openid 用户openid
     */
    public void deleteSessionKey(String openid) {
        String key = WX_SESSION_PREFIX + openid;
        redisService.del(key);
        log.debug("删除session_key, openid: {}", openid);
    }

    /**
     * 延长session_key有效期
     * @param openid 用户openid
     */
    public void extendSessionKey(String openid) {
        String key = WX_SESSION_PREFIX + openid;
        if (redisService.hasKey(key)) {
            redisService.expire(key, SESSION_EXPIRE, TimeUnit.SECONDS);
            log.debug("延长session_key有效期, openid: {}", openid);
        }
    }

    // ==================== 用户Token 相关 ====================

    /**
     * 保存用户登录token
     * @param token 登录token
     * @param userId 用户ID
     */
    public void saveUserToken(String token, Long userId) {
        String key = USER_TOKEN_PREFIX + token;
        redisService.set(key, userId, TOKEN_EXPIRE, TimeUnit.SECONDS);
        log.debug("保存用户token, userId: {}, expire: {}秒", userId, TOKEN_EXPIRE);
    }

    /**
     * 获取token对应的用户ID
     * @param token 登录token
     * @return 用户ID
     */
    public Long getUserIdByToken(String token) {
        String key = USER_TOKEN_PREFIX + token;
        Object value = redisService.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 删除用户token
     * @param token 登录token
     */
    public void deleteUserToken(String token) {
        String key = USER_TOKEN_PREFIX + token;
        redisService.del(key);
        log.debug("删除用户token, token: {}", token);
    }

    /**
     * 延长token有效期
     * @param token 登录token
     */
    public void extendUserToken(String token) {
        String key = USER_TOKEN_PREFIX + token;
        if (redisService.hasKey(key)) {
            redisService.expire(key, TOKEN_EXPIRE, TimeUnit.SECONDS);
            log.debug("延长token有效期, token: {}", token);
        }
    }

    /**
     * 验证token是否有效
     * @param token 登录token
     * @return 是否有效
     */
    public boolean isTokenValid(String token) {
        String key = USER_TOKEN_PREFIX + token;
        return redisService.hasKey(key);
    }

    // ==================== 微信AccessToken 相关 ====================

    /**
     * 保存微信access_token（如果需要调用其他微信接口）
     * @param accessToken access_token
     */
    public void saveWxAccessToken(String accessToken) {
        String key = WX_TOKEN_PREFIX + "access";
        redisService.set(key, accessToken, ACCESS_TOKEN_EXPIRE, TimeUnit.SECONDS);
        log.debug("保存微信access_token, expire: {}秒", ACCESS_TOKEN_EXPIRE);
    }

    /**
     * 获取微信access_token
     * @return access_token
     */
    public String getWxAccessToken() {
        String key = WX_TOKEN_PREFIX + "access";
        return redisService.getString(key);
    }

    // ==================== 验证码 相关 ====================

    /**
     * 保存短信验证码
     * @param phone 手机号
     * @param code 验证码
     * @param expireSeconds 过期时间（秒）
     */
    public void saveSmsCode(String phone, String code, long expireSeconds) {
        String key = "sms_code:" + phone;
        redisService.set(key, code, expireSeconds, TimeUnit.SECONDS);
        log.debug("保存短信验证码, phone: {}, code: {}", phone, code);
    }

    /**
     * 验证短信验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    public boolean verifySmsCode(String phone, String code) {
        String key = "sms_code:" + phone;
        String savedCode = redisService.getString(key);
        if (savedCode == null) {
            return false;
        }

        boolean isValid = savedCode.equals(code);
        if (isValid) {
            // 验证成功后删除验证码，防止重复使用
            redisService.del(key);
        }
        return isValid;
    }

    // ==================== 频率限制 相关 ====================

    /**
     * 检查操作频率
     * @param key 操作key（如：login:127.0.0.1）
     * @param limit 限制次数
     * @param period 时间周期（秒）
     * @return 是否超过限制
     */
    public boolean checkRateLimit(String key, int limit, int period) {
        String redisKey = "rate_limit:" + key;
        Long count = redisService.incr(redisKey, 1);

        if (count == 1) {
            // 第一次设置过期时间
            redisService.expire(redisKey, period, TimeUnit.SECONDS);
        }

        return count != null && count <= limit;
    }

    /**
     * 获取剩余操作次数
     * @param key 操作key
     * @return 剩余次数
     */
    public Long getRemainingLimit(String key) {
        String redisKey = "rate_limit:" + key;
        String value = redisService.getString(redisKey);
        if (value == null) {
            return null;
        }
        return Long.parseLong(value);
    }
}