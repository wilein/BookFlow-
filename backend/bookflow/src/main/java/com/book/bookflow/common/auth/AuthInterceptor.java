package com.book.bookflow.common.auth;

import com.alibaba.fastjson.JSON;
import com.book.bookflow.common.Result;
import com.book.bookflow.exception.CustomerException;
import com.book.bookflow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/user/auth/wechat",
        "/user/auth/dev-login",
        "/common/banner/list",
        "/book/list",
        "/book/category",
        "/book/detail",
        "/book/search",
        "/path/list",
        "/path/detail",
        "/annotation/list",
        "/community/feed",
        "/community/activity",
        "/community/post/detail",
        "/community/post/comment/list",
        "/order/pay/notify/wechat",
        "/resource/list"
    );

    private final UserService userService;

    @Value("${app.security.dev-login-enabled:false}")
    private boolean devLoginEnabled;

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        String token = extractToken(request.getHeader("Authorization"));
        if (isPublicPath(requestUri)) {
            if (!token.isBlank()) {
                try {
                    Map<String, Object> session = userService.checkLoginStatus(token);
                    Map<String, Object> userInfo = castMap(session.get("userInfo"));
                    Long userId = asLong(userInfo.get("userId"));
                    if (userId != null) {
                        AuthContext.setUserId(userId);
                        AuthContext.setToken(String.valueOf(session.get("token")));
                        response.setHeader("X-Access-Token", String.valueOf(session.get("token")));
                        response.setHeader("X-Token-Expire-At", String.valueOf(session.get("expireAt")));
                    }
                } catch (RuntimeException ignored) {
                    AuthContext.clear();
                }
            }
            return true;
        }

        if (token.isBlank()) {
            writeUnauthorized(response, "请先登录");
            return false;
        }

        try {
            Map<String, Object> session = userService.checkLoginStatus(token);
            Map<String, Object> userInfo = castMap(session.get("userInfo"));
            Long userId = asLong(userInfo.get("userId"));
            if (userId == null) {
                writeUnauthorized(response, "登录状态已过期");
                return false;
            }
            AuthContext.setUserId(userId);
            AuthContext.setToken(String.valueOf(session.get("token")));
            response.setHeader("X-Access-Token", String.valueOf(session.get("token")));
            response.setHeader("X-Token-Expire-At", String.valueOf(session.get("expireAt")));
            return true;
        } catch (CustomerException exception) {
            writeUnauthorized(response, exception.getMsg());
            return false;
        } catch (RuntimeException exception) {
            writeUnauthorized(response, "登录状态已过期");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublicPath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return false;
        }
        String normalizedUri = requestUri.replace('\\', '/').replaceAll("/{2,}", "/");
        if (normalizedUri.startsWith("/uploads/profile/student-card/")) {
            return false;
        }
        if (normalizedUri.startsWith("/uploads/")) {
            return true;
        }
        if (normalizedUri.startsWith("/user/auth/dev-login") && !devLoginEnabled) {
            return false;
        }
        return PUBLIC_PATHS.stream().anyMatch(normalizedUri::startsWith);
    }

    private String extractToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        String value = authorization.trim();
        if (value.startsWith("Bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.error("401", message)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
