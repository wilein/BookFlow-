package com.book.bookflow.common.auth;

public final class AuthContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static Long requireUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new IllegalStateException("Current user is missing");
        }
        return userId;
    }

    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        TOKEN_HOLDER.remove();
    }
}
