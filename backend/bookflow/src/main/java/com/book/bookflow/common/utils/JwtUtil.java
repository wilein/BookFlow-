package com.book.bookflow.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String SECRET_KEY = "heyuan";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);

    // 默认过期时间：12小时
    private static final long DEFAULT_EXPIRE_TIME = 1000 * 60 * 60 * 12;

    /**
     * 生成token（使用默认过期时间）
     */
    public static String genToken(Map<String, Object> claims) {
        return genToken(claims, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 生成token（自定义过期时间）
     * @param claims 自定义数据
     * @param expireTime 过期时间（毫秒）
     */
    public static String genToken(Map<String, Object> claims, long expireTime) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + expireTime))
                .sign(ALGORITHM);
    }

    /**
     * 解析token，返回claims数据
     */
    public static Map<String, Object> parseToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(ALGORITHM)
                    .build()
                    .verify(token);
            return jwt.getClaim("claims").asMap();
        } catch (Exception e) {
            throw new RuntimeException("Token解析失败: " + e.getMessage());
        }
    }

    /**
     * 验证token是否有效
     */
    public static boolean verifyToken(String token) {
        try {
            JWT.require(ALGORITHM)
                    .build()
                    .verify(token);
            return true;
        } catch (TokenExpiredException e) {
            // Token过期
            return false;
        } catch (JWTVerificationException e) {
            // Token无效
            return false;
        } catch (Exception e) {
            // 其他异常
            return false;
        }
    }

    /**
     * 验证token是否有效（不抛出异常）
     */
    public static boolean verifyTokenSilently(String token) {
        try {
            return verifyToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取token的过期时间
     */
    public static Date getExpirationDate(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查token是否即将过期（例如：30分钟内过期）
     */
    public static boolean isTokenAboutToExpire(String token, long minutesBefore) {
        try {
            Date expiration = getExpirationDate(token);
            if (expiration == null) {
                return true;
            }

            long timeLeft = expiration.getTime() - System.currentTimeMillis();
            return timeLeft <= (minutesBefore * 60 * 1000);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新token（延长过期时间）
     */
    public static String refreshToken(String token, long newExpireTime) {
        try {
            // 解析原token中的claims
            Map<String, Object> claims = parseToken(token);

            // 生成新token
            return genToken(claims, newExpireTime);
        } catch (Exception e) {
            throw new RuntimeException("刷新token失败: " + e.getMessage());
        }
    }

    /**
     * 获取token中的用户ID
     */
    public static Long getUserId(String token) {
        try {
            Map<String, Object> claims = parseToken(token);
            Object userId = claims.get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取token中的openid
     */
    public static String getOpenid(String token) {
        try {
            Map<String, Object> claims = parseToken(token);
            Object openid = claims.get("openid");
            return openid != null ? openid.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从token中获取特定claim值
     */
    public static Object getClaim(String token, String claimName) {
        try {
            Map<String, Object> claims = parseToken(token);
            return claims.get(claimName);
        } catch (Exception e) {
            return null;
        }
    }
}