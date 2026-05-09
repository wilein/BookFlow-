package com.book.bookflow.common.config;

import com.book.bookflow.common.utils.AdminPasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

@Component
public class SchemaUpgradeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgradeRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public SchemaUpgradeRunner(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn(
            "learning_path",
            "source_path_id",
            "ALTER TABLE `learning_path` ADD COLUMN `source_path_id` BIGINT NULL AFTER `book_id`"
        );
        ensureColumn(
            "learning_path",
            "cover_image_status",
            "ALTER TABLE `learning_path` ADD COLUMN `cover_image_status` TINYINT DEFAULT 0 COMMENT '封面审核状态：0无需审核 1待审核 2已通过 3已驳回' AFTER `cover_image`"
        );
        ensureColumn(
            "path_node",
            "learning_goal",
            "ALTER TABLE `path_node` ADD COLUMN `learning_goal` VARCHAR(500) DEFAULT NULL AFTER `description`"
        );
        ensureColumn(
            "path_node",
            "learning_method",
            "ALTER TABLE `path_node` ADD COLUMN `learning_method` VARCHAR(800) DEFAULT NULL AFTER `learning_goal`"
        );
        ensureColumn(
            "path_node",
            "deliverable",
            "ALTER TABLE `path_node` ADD COLUMN `deliverable` VARCHAR(500) DEFAULT NULL AFTER `learning_method`"
        );
        ensureColumn(
            "path_node",
            "steps_json",
            "ALTER TABLE `path_node` ADD COLUMN `steps_json` JSON NULL AFTER `deliverable`"
        );
        ensureColumn(
            "resource",
            "bind_type",
            "ALTER TABLE `resource` ADD COLUMN `bind_type` VARCHAR(32) NOT NULL DEFAULT 'none' AFTER `book_id`"
        );
        ensureColumn(
            "resource",
            "bind_id",
            "ALTER TABLE `resource` ADD COLUMN `bind_id` BIGINT NULL AFTER `bind_type`"
        );
        ensureColumn(
            "post",
            "shared_path_id",
            "ALTER TABLE `post` ADD COLUMN `shared_path_id` BIGINT NULL AFTER `type`"
        );
        ensureVarcharLength(
            "user_browse_history",
            "route_url",
            2048,
            "ALTER TABLE `user_browse_history` MODIFY COLUMN `route_url` VARCHAR(2048) DEFAULT NULL"
        );
        ensureVarcharLength(
            "book",
            "cover_images",
            2048,
            "ALTER TABLE `book` MODIFY COLUMN `cover_images` VARCHAR(2048) DEFAULT NULL"
        );
        ensureTable(
            "notification",
            """
                CREATE TABLE IF NOT EXISTS `notification` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `type` VARCHAR(32) NOT NULL,
                  `title` VARCHAR(200) NOT NULL,
                  `content` VARCHAR(500) NOT NULL,
                  `route_url` VARCHAR(255) DEFAULT NULL,
                  `is_read` TINYINT DEFAULT 0,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `is_deleted` TINYINT DEFAULT 0,
                  PRIMARY KEY (`id`),
                  KEY `idx_notification_user` (`user_id`),
                  KEY `idx_notification_user_read` (`user_id`, `is_read`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        ensureTable(
            "order_issue",
            """
                CREATE TABLE IF NOT EXISTS `order_issue` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `order_id` BIGINT NOT NULL,
                  `user_id` BIGINT NOT NULL,
                  `type` TINYINT NOT NULL,
                  `content` VARCHAR(500) NOT NULL,
                  `reply_content` VARCHAR(500) DEFAULT NULL,
                  `reply_user_id` BIGINT DEFAULT NULL,
                  `status` TINYINT DEFAULT 0,
                  `reply_time` DATETIME DEFAULT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `is_deleted` TINYINT DEFAULT 0,
                  PRIMARY KEY (`id`),
                  KEY `idx_order_issue_order` (`order_id`),
                  KEY `idx_order_issue_user` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        ensureTable(
            "content_report",
            """
                CREATE TABLE IF NOT EXISTS `content_report` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `target_type` VARCHAR(32) NOT NULL,
                  `target_id` BIGINT NOT NULL,
                  `reason_type` VARCHAR(64) NOT NULL,
                  `content` VARCHAR(500) NOT NULL,
                  `status` TINYINT DEFAULT 0,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `is_deleted` TINYINT DEFAULT 0,
                  PRIMARY KEY (`id`),
                  KEY `idx_content_report_user` (`user_id`),
                  KEY `idx_content_report_target` (`target_type`, `target_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        ensureTable(
            "admin_user",
            """
                CREATE TABLE IF NOT EXISTS `admin_user` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `username` VARCHAR(64) NOT NULL,
                  `password_hash` VARCHAR(128) NOT NULL,
                  `real_name` VARCHAR(64) DEFAULT NULL,
                  `role` VARCHAR(32) NOT NULL DEFAULT 'admin',
                  `status` TINYINT DEFAULT 1,
                  `last_login_time` DATETIME DEFAULT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `is_deleted` TINYINT DEFAULT 0,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_admin_user_username` (`username`),
                  KEY `idx_admin_user_status` (`status`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        ensureVarcharLength(
            "admin_user",
            "password_hash",
            255,
            "ALTER TABLE `admin_user` MODIFY COLUMN `password_hash` VARCHAR(255) NOT NULL"
        );
        ensureTable(
            "admin_operation_log",
            """
                CREATE TABLE IF NOT EXISTS `admin_operation_log` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `admin_id` BIGINT NOT NULL,
                  `module` VARCHAR(64) NOT NULL,
                  `action` VARCHAR(64) NOT NULL,
                  `target_id` BIGINT DEFAULT NULL,
                  `before_data` LONGTEXT,
                  `after_data` LONGTEXT,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_admin_operation_admin` (`admin_id`),
                  KEY `idx_admin_operation_module` (`module`),
                  KEY `idx_admin_operation_target` (`target_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        ensureTable(
            "cart_item",
            """
                CREATE TABLE IF NOT EXISTS `cart_item` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `book_id` BIGINT NOT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `is_deleted` TINYINT DEFAULT 0,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_cart_user_book_active` (`user_id`, `book_id`, `is_deleted`),
                  KEY `idx_cart_user` (`user_id`, `is_deleted`),
                  KEY `idx_cart_book` (`book_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        ensureIndex(
            "book",
            "idx_book_public_category",
            "ALTER TABLE `book` ADD INDEX `idx_book_public_category` (`is_deleted`, `status`, `category`, `id`)"
        );
        ensureIndex(
            "post",
            "idx_post_feed",
            "ALTER TABLE `post` ADD INDEX `idx_post_feed` (`is_deleted`, `id`)"
        );
        ensureIndex(
            "comment",
            "idx_comment_post",
            "ALTER TABLE `comment` ADD INDEX `idx_comment_post` (`post_id`, `is_deleted`, `id`)"
        );
        ensureIndex(
            "order",
            "idx_order_buyer_status",
            "ALTER TABLE `order` ADD INDEX `idx_order_buyer_status` (`buyer_id`, `status`, `is_deleted`, `id`)"
        );
        ensureIndex(
            "order",
            "idx_order_seller_status",
            "ALTER TABLE `order` ADD INDEX `idx_order_seller_status` (`seller_id`, `status`, `is_deleted`, `id`)"
        );
        ensureIndex(
            "chat_session",
            "idx_chat_session_participant",
            "ALTER TABLE `chat_session` ADD INDEX `idx_chat_session_participant` (`buyer_id`, `seller_id`, `is_deleted`, `last_message_time`)"
        );
        ensureIndex(
            "chat_message",
            "idx_chat_message_poll",
            "ALTER TABLE `chat_message` ADD INDEX `idx_chat_message_poll` (`session_id`, `is_deleted`, `id`)"
        );
        ensureIndex(
            "chat_message",
            "idx_chat_message_unread",
            "ALTER TABLE `chat_message` ADD INDEX `idx_chat_message_unread` (`session_id`, `sender_id`, `is_read`, `is_deleted`)"
        );
        seedDefaultAdmin();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
            Integer.class,
            tableName,
            columnName
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """,
            Integer.class,
            tableName,
            indexName
        );
        return count != null && count > 0;
    }

    private boolean varcharLengthAtLeast(String tableName, String columnName, int minLength) {
        Integer currentLength = jdbcTemplate.queryForObject(
            """
                SELECT CHARACTER_MAXIMUM_LENGTH
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
            Integer.class,
            tableName,
            columnName
        );
        return currentLength != null && currentLength >= minLength;
    }

    private void ensureTable(String tableName, String createSql) {
        executeIfMissing(tableName, () -> tableExists(tableName), createSql);
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        executeIfMissing(tableName + "." + columnName, () -> columnExists(tableName, columnName), alterSql);
    }

    private void ensureIndex(String tableName, String indexName, String alterSql) {
        if (!tableExists(tableName)) {
            return;
        }
        executeIfMissing(tableName + "." + indexName, () -> indexExists(tableName, indexName), alterSql);
    }

    private void ensureVarcharLength(String tableName, String columnName, int minLength, String alterSql) {
        executeIfMissing(
            tableName + "." + columnName + ".length>=" + minLength,
            () -> columnExists(tableName, columnName) && varcharLengthAtLeast(tableName, columnName, minLength),
            alterSql
        );
    }

    private void executeIfMissing(String target, BooleanSupplier existsCheck, String sql) {
        if (existsCheck.getAsBoolean()) {
            return;
        }
        log.warn("Schema target {} is missing. Applying upgrade.", target);
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception exception) {
            log.error("Failed to apply schema upgrade for {}.", target, exception);
            throw new IllegalStateException("Failed to apply schema upgrade for " + target, exception);
        }
    }

    private void seedDefaultAdmin() {
        if (!tableExists("admin_user")) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM `admin_user` WHERE `is_deleted` = 0",
            Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        String username = env("BOOKFLOW_ADMIN_USERNAME");
        String password = env("BOOKFLOW_ADMIN_PASSWORD");
        if (isProduction()) {
            if (username.isBlank() || password.isBlank()) {
                throw new IllegalStateException("No admin user found. BOOKFLOW_ADMIN_USERNAME and BOOKFLOW_ADMIN_PASSWORD are required in production.");
            }
        } else {
            username = username.isBlank() ? "admin" : username;
            password = password.isBlank() ? "123456" : password;
            log.warn("No admin user found. Creating development admin account: {}.", username);
        }
        jdbcTemplate.update(
            """
                INSERT INTO `admin_user`
                (`username`, `password_hash`, `real_name`, `role`, `status`, `create_time`, `update_time`, `is_deleted`)
                VALUES (?, ?, ?, ?, 1, NOW(), NOW(), 0)
                """,
            username,
            AdminPasswordUtil.hashPassword(username, password),
            "超级管理员",
            "super_admin"
        );
    }

    private boolean isProduction() {
        String profiles = String.join(",", environment.getActiveProfiles());
        String env = env("BOOKFLOW_ENV");
        String value = (profiles + "," + env).toLowerCase();
        return value.contains("prod") || value.contains("production");
    }

    private String env(String name) {
        String value = environment.getProperty(name);
        return value == null ? "" : value.trim();
    }
}
