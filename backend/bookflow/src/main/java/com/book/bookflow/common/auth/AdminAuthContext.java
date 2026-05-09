package com.book.bookflow.common.auth;

import com.book.bookflow.exception.CustomerException;

public class AdminAuthContext {

    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private AdminAuthContext() {
    }

    public static void set(Long adminId, String username, String role, String token) {
        ADMIN_ID.set(adminId);
        USERNAME.set(username);
        ROLE.set(role);
        TOKEN.set(token);
    }

    public static Long getAdminId() {
        return ADMIN_ID.get();
    }

    public static Long requireAdminId() {
        Long adminId = getAdminId();
        if (adminId == null) {
            throw new CustomerException("401", "请先登录后台");
        }
        return adminId;
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static String getToken() {
        return TOKEN.get();
    }

    public static void clear() {
        ADMIN_ID.remove();
        USERNAME.remove();
        ROLE.remove();
        TOKEN.remove();
    }
}
