package com.book.bookflow.common.config;

import com.book.bookflow.exception.CustomerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class WxSessionRedisService {

    private static final String WX_SESSION_PREFIX = "wx_session:";
    private static final String WX_TOKEN_PREFIX = "wx_token:";
    private static final String USER_TOKEN_PREFIX = "user_token:";
    private static final long SESSION_EXPIRE = 7200;
    private static final long TOKEN_EXPIRE = 15 * 24 * 3600;
    private static final long ACCESS_TOKEN_EXPIRE = 7100;

    private final RedisService redisService;

    public WxSessionRedisService(RedisService redisService) {
        this.redisService = redisService;
    }

    public void saveSessionKey(String openid, String sessionKey) {
        boolean success = redisService.setString(WX_SESSION_PREFIX + openid, sessionKey, SESSION_EXPIRE, TimeUnit.SECONDS);
        if (!success) {
            throw new CustomerException("500", "登录状态保存失败，请检查 Redis");
        }
    }

    public String getSessionKey(String openid) {
        return redisService.getString(WX_SESSION_PREFIX + openid);
    }

    public void deleteSessionKey(String openid) {
        redisService.del(WX_SESSION_PREFIX + openid);
    }

    public void extendSessionKey(String openid) {
        String key = WX_SESSION_PREFIX + openid;
        if (redisService.hasKey(key)) {
            redisService.expire(key, SESSION_EXPIRE, TimeUnit.SECONDS);
        }
    }

    public void saveUserToken(String token, Long userId) {
        boolean success = redisService.setString(USER_TOKEN_PREFIX + token, String.valueOf(userId), TOKEN_EXPIRE, TimeUnit.SECONDS);
        if (!success) {
            throw new CustomerException("500", "登录状态保存失败，请检查 Redis");
        }
    }

    public Long getUserIdByToken(String token) {
        String value = redisService.getString(USER_TOKEN_PREFIX + token);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public void deleteUserToken(String token) {
        redisService.del(USER_TOKEN_PREFIX + token);
    }

    public void extendUserToken(String token) {
        String key = USER_TOKEN_PREFIX + token;
        if (redisService.hasKey(key)) {
            redisService.expire(key, TOKEN_EXPIRE, TimeUnit.SECONDS);
        }
    }

    public boolean isTokenValid(String token) {
        return redisService.hasKey(USER_TOKEN_PREFIX + token);
    }

    public long getTokenExpireSeconds() {
        return TOKEN_EXPIRE;
    }

    public void saveWxAccessToken(String accessToken) {
        redisService.setString(WX_TOKEN_PREFIX + "access", accessToken, ACCESS_TOKEN_EXPIRE, TimeUnit.SECONDS);
    }

    public String getWxAccessToken() {
        return redisService.getString(WX_TOKEN_PREFIX + "access");
    }

    public void saveSmsCode(String phone, String code, long expireSeconds) {
        redisService.setString("sms_code:" + phone, code, expireSeconds, TimeUnit.SECONDS);
    }

    public boolean verifySmsCode(String phone, String code) {
        String key = "sms_code:" + phone;
        String savedCode = redisService.getString(key);
        if (savedCode == null) {
            return false;
        }
        boolean valid = savedCode.equals(code);
        if (valid) {
            redisService.del(key);
        }
        return valid;
    }

    public boolean checkRateLimit(String key, int limit, int period) {
        String redisKey = "rate_limit:" + key;
        Long count = redisService.incr(redisKey, 1);
        if (count == 1) {
            redisService.expire(redisKey, period, TimeUnit.SECONDS);
        }
        return count != null && count <= limit;
    }

    public Long getRemainingLimit(String key) {
        String value = redisService.getString("rate_limit:" + key);
        if (value == null) {
            return null;
        }
        return Long.parseLong(value);
    }
}
