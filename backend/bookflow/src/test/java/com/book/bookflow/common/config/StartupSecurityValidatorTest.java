package com.book.bookflow.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartupSecurityValidatorTest {

    @Test
    void developmentProfileAllowsLocalDefaults() {
        MockEnvironment environment = new MockEnvironment();
        StartupSecurityValidator validator = new StartupSecurityValidator(environment);

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void productionProfileRequiresCriticalEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        StartupSecurityValidator validator = new StartupSecurityValidator(environment);

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void productionProfileRejectsKnownWeakDefaults() {
        MockEnvironment environment = productionEnvironment();
        environment.setProperty("BOOKFLOW_DB_PASSWORD", "123456");
        StartupSecurityValidator validator = new StartupSecurityValidator(environment);

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void productionProfileAcceptsCompleteStrongConfig() {
        MockEnvironment environment = productionEnvironment();
        StartupSecurityValidator validator = new StartupSecurityValidator(environment);

        assertDoesNotThrow(() -> validator.run(null));
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        environment.setProperty("BOOKFLOW_JWT_SECRET", "bookflow-production-secret-32-chars");
        environment.setProperty("BOOKFLOW_DB_URL", "jdbc:mysql://db.example.com:3306/bookflow");
        environment.setProperty("BOOKFLOW_DB_USERNAME", "bookflow");
        environment.setProperty("BOOKFLOW_DB_PASSWORD", "strong-db-password");
        environment.setProperty("weixin.appid", "wx-production-appid");
        environment.setProperty("weixin.secret", "wx-production-secret");
        environment.setProperty("app.security.dev-login-enabled", "false");
        environment.setProperty("pay.mode", "mock");
        return environment;
    }
}
