package com.book.bookflow.common.auth;

import com.alibaba.fastjson.JSON;
import com.book.bookflow.common.AdminResult;
import com.book.bookflow.common.utils.JwtUtil;
import com.book.bookflow.entity.AdminUser;
import com.book.bookflow.mapper.AdminUserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/admin/auth/login"
    );

    private final AdminUserMapper adminUserMapper;

    public AdminAuthInterceptor(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String requestUri = request.getRequestURI();
        if (PUBLIC_PATHS.stream().anyMatch(requestUri::startsWith)) {
            return true;
        }

        String token = extractToken(request.getHeader("Authorization"));
        if (token.isBlank()) {
            token = extractToken(request.getParameter("token"));
        }
        if (token.isBlank()) {
            writeUnauthorized(response, "请先登录后台");
            return false;
        }
        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);
            if (!"admin".equals(String.valueOf(claims.get("type")))) {
                writeUnauthorized(response, "身份类型不正确");
                return false;
            }
            Long adminId = asLong(claims.get("adminId"));
            if (adminId == null) {
                writeUnauthorized(response, "后台登录状态已失效");
                return false;
            }
            AdminUser adminUser = adminUserMapper.selectOneByQuery(
                QueryWrapper.create()
                    .where("id = ?", adminId)
                    .and("is_deleted = 0")
                    .limit(1)
            );
            if (adminUser == null || !Integer.valueOf(1).equals(adminUser.getStatus())) {
                writeUnauthorized(response, "管理员账号不可用");
                return false;
            }
            AdminAuthContext.set(adminId, adminUser.getUsername(), adminUser.getRole(), token);
            return true;
        } catch (RuntimeException exception) {
            writeUnauthorized(response, "后台登录状态已失效");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminAuthContext.clear();
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
        response.getWriter().write(JSON.toJSONString(AdminResult.error(401, message)));
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
