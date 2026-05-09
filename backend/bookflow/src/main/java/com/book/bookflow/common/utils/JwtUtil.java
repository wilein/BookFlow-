package com.book.bookflow.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String DEFAULT_SECRET_KEY = "bookflow-dev-secret";
    private static final String SECRET_KEY = resolveSecretKey();
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);
    private static final long DEFAULT_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 15;

    private JwtUtil() {
    }

    private static String resolveSecretKey() {
        String envSecret = System.getenv("BOOKFLOW_JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) {
            return envSecret.trim();
        }
        String propertySecret = System.getProperty("bookflow.jwt.secret");
        if (propertySecret != null && !propertySecret.isBlank()) {
            return propertySecret.trim();
        }
        if (isProductionProfile()) {
            throw new IllegalStateException("BOOKFLOW_JWT_SECRET must be configured in production");
        }
        return DEFAULT_SECRET_KEY;
    }

    private static boolean isProductionProfile() {
        String profiles = System.getProperty("spring.profiles.active", "");
        if (profiles == null || profiles.isBlank()) {
            profiles = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        String env = System.getenv("BOOKFLOW_ENV");
        String value = ((profiles == null ? "" : profiles) + "," + (env == null ? "" : env)).toLowerCase();
        return value.contains("prod") || value.contains("production");
    }

    public static String genToken(Map<String, Object> claims) {
        return genToken(claims, DEFAULT_EXPIRE_TIME);
    }

    public static String genToken(Map<String, Object> claims, long expireTime) {
        return JWT.create()
            .withClaim("claims", claims)
            .withExpiresAt(new Date(System.currentTimeMillis() + expireTime))
            .sign(ALGORITHM);
    }

    public static Map<String, Object> parseToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(ALGORITHM).build().verify(token);
            return jwt.getClaim("claims").asMap();
        } catch (Exception exception) {
            throw new RuntimeException("Token解析失败: " + exception.getMessage());
        }
    }

    public static boolean verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    public static boolean verifyTokenSilently(String token) {
        try {
            return verifyToken(token);
        } catch (Exception exception) {
            return false;
        }
    }

    public static Date getExpirationDate(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt();
        } catch (Exception exception) {
            return null;
        }
    }

    public static boolean isTokenAboutToExpire(String token, long minutesBefore) {
        try {
            Date expiration = getExpirationDate(token);
            if (expiration == null) {
                return true;
            }
            long timeLeft = expiration.getTime() - System.currentTimeMillis();
            return timeLeft <= (minutesBefore * 60 * 1000);
        } catch (Exception exception) {
            return true;
        }
    }

    public static String refreshToken(String token, long newExpireTime) {
        Map<String, Object> claims = parseToken(token);
        return genToken(claims, newExpireTime);
    }

    public static Long getUserId(String token) {
        try {
            Object userId = parseToken(token).get("userId");
            if (userId instanceof Number number) {
                return number.longValue();
            }
            return userId == null ? null : Long.parseLong(String.valueOf(userId));
        } catch (Exception exception) {
            return null;
        }
    }

    public static String getOpenid(String token) {
        try {
            Object openid = parseToken(token).get("openid");
            return openid == null ? null : String.valueOf(openid);
        } catch (Exception exception) {
            return null;
        }
    }

    public static Object getClaim(String token, String claimName) {
        try {
            return parseToken(token).get(claimName);
        } catch (Exception exception) {
            return null;
        }
    }
}
