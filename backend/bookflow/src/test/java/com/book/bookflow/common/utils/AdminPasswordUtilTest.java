package com.book.bookflow.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPasswordUtilTest {

    @Test
    void bcryptHashMatchesAndDoesNotNeedRehash() {
        String hash = AdminPasswordUtil.hashPassword("admin", "strong-password");

        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(AdminPasswordUtil.matches("admin", "strong-password", hash));
        assertFalse(AdminPasswordUtil.matches("admin", "wrong-password", hash));
        assertFalse(AdminPasswordUtil.needsRehash(hash));
    }

    @Test
    void legacySha256HashStillMatchesButNeedsRehash() {
        String legacyHash = AdminPasswordUtil.legacyHashPassword("admin", "123456");

        assertTrue(AdminPasswordUtil.matches("admin", "123456", legacyHash));
        assertFalse(AdminPasswordUtil.matches("admin", "1234567", legacyHash));
        assertTrue(AdminPasswordUtil.needsRehash(legacyHash));
    }
}
