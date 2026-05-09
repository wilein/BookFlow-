package com.book.bookflow.common.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StartupSecurityValidator implements ApplicationRunner, Ordered {

    private static final String DEFAULT_DB_PASSWORD = "123456";
    private static final String DEFAULT_WECHAT_APPID = "wxf036e74d6a11e752";
    private static final String DEFAULT_WECHAT_SECRET = "dac60ddd83c608baf819af772090c7a3";
    private static final String DEFAULT_JWT_SECRET = "bookflow-dev-secret";

    private final Environment environment;

    public StartupSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProduction()) {
            return;
        }
        requireNotBlank("BOOKFLOW_JWT_SECRET");
        requireNotBlank("BOOKFLOW_DB_URL");
        requireNotBlank("BOOKFLOW_DB_USERNAME");
        requireNotBlank("BOOKFLOW_DB_PASSWORD");
        rejectDefault("BOOKFLOW_DB_PASSWORD", DEFAULT_DB_PASSWORD);
        requireNotDefault("weixin.appid", DEFAULT_WECHAT_APPID, "BOOKFLOW_WECHAT_APPID");
        requireNotDefault("weixin.secret", DEFAULT_WECHAT_SECRET, "BOOKFLOW_WECHAT_SECRET");
        rejectDefault("BOOKFLOW_JWT_SECRET", DEFAULT_JWT_SECRET);
        if (environment.getProperty("app.security.dev-login-enabled", Boolean.class, false)) {
            throw new IllegalStateException("BOOKFLOW_DEV_LOGIN_ENABLED must not be true in production");
        }
        if ("wechat".equalsIgnoreCase(environment.getProperty("pay.mode", ""))) {
            List.of(
                "BOOKFLOW_WECHAT_MCH_ID",
                "BOOKFLOW_WECHAT_MCH_SERIAL_NO",
                "BOOKFLOW_WECHAT_API_V3_KEY",
                "BOOKFLOW_WECHAT_PRIVATE_KEY_PATH",
                "BOOKFLOW_WECHAT_PLATFORM_CERT_PATH",
                "BOOKFLOW_WECHAT_NOTIFY_URL"
            ).forEach(this::requireNotBlank);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isProduction() {
        String activeProfiles = String.join(",", environment.getActiveProfiles());
        String env = environment.getProperty("BOOKFLOW_ENV", "");
        String value = (activeProfiles + "," + env).toLowerCase();
        return Arrays.stream(value.split(",")).anyMatch(item -> item.contains("prod") || item.contains("production"));
    }

    private void requireNotDefault(String propertyName, String defaultValue, String envName) {
        String value = environment.getProperty(propertyName, "");
        if (value.isBlank() || defaultValue.equals(value.trim())) {
            throw new IllegalStateException(envName + " must be configured in production");
        }
    }

    private void requireNotBlank(String envName) {
        String value = environment.getProperty(envName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envName + " must be configured in production");
        }
    }

    private void rejectDefault(String envName, String defaultValue) {
        String value = environment.getProperty(envName);
        if (defaultValue.equals(value)) {
            throw new IllegalStateException(envName + " must not use development default in production");
        }
    }
}
