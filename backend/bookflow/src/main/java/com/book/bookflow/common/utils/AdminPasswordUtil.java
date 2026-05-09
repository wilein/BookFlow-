package com.book.bookflow.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCrypt;

public class AdminPasswordUtil {

    private static final String PASSWORD_SALT = "bookflow-admin-v1";
    private static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final Pattern LEGACY_SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private AdminPasswordUtil() {
    }

    public static String hashPassword(String username, String rawPassword) {
        return BCRYPT_PREFIX + BCrypt.hashpw(normalize(rawPassword), BCrypt.gensalt(12));
    }

    public static boolean matches(String username, String rawPassword, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        String normalizedHash = passwordHash.trim();
        if (normalizedHash.startsWith(BCRYPT_PREFIX)) {
            return BCrypt.checkpw(normalize(rawPassword), normalizedHash.substring(BCRYPT_PREFIX.length()));
        }
        if (normalizedHash.startsWith("$2a$") || normalizedHash.startsWith("$2b$") || normalizedHash.startsWith("$2y$")) {
            return BCrypt.checkpw(normalize(rawPassword), normalizedHash);
        }
        if (LEGACY_SHA256.matcher(normalizedHash).matches()) {
            return normalizedHash.equals(legacyHashPassword(username, rawPassword));
        }
        return false;
    }

    public static boolean needsRehash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return true;
        }
        return !passwordHash.trim().startsWith(BCRYPT_PREFIX);
    }

    public static String legacyHashPassword(String username, String rawPassword) {
        String source = normalize(username) + ":" + normalize(rawPassword) + ":" + PASSWORD_SALT;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
