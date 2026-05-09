SET @db = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'learning_path' AND COLUMN_NAME = 'source_path_id'
  ),
  'SELECT 1',
  'ALTER TABLE `learning_path` ADD COLUMN `source_path_id` BIGINT NULL AFTER `book_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'learning_path' AND COLUMN_NAME = 'cover_image_status'
  ),
  'SELECT 1',
  'ALTER TABLE `learning_path` ADD COLUMN `cover_image_status` TINYINT DEFAULT 0 COMMENT ''封面审核状态：0无需审核 1待审核 2已通过 3已驳回'' AFTER `cover_image`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'path_node' AND COLUMN_NAME = 'learning_goal'
  ),
  'SELECT 1',
  'ALTER TABLE `path_node` ADD COLUMN `learning_goal` VARCHAR(500) DEFAULT NULL AFTER `description`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'path_node' AND COLUMN_NAME = 'learning_method'
  ),
  'SELECT 1',
  'ALTER TABLE `path_node` ADD COLUMN `learning_method` VARCHAR(800) DEFAULT NULL AFTER `learning_goal`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'path_node' AND COLUMN_NAME = 'deliverable'
  ),
  'SELECT 1',
  'ALTER TABLE `path_node` ADD COLUMN `deliverable` VARCHAR(500) DEFAULT NULL AFTER `learning_method`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'path_node' AND COLUMN_NAME = 'steps_json'
  ),
  'SELECT 1',
  'ALTER TABLE `path_node` ADD COLUMN `steps_json` JSON NULL AFTER `deliverable`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'resource' AND COLUMN_NAME = 'bind_type'
  ),
  'SELECT 1',
  'ALTER TABLE `resource` ADD COLUMN `bind_type` VARCHAR(32) NOT NULL DEFAULT ''none'' AFTER `book_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'resource' AND COLUMN_NAME = 'bind_id'
  ),
  'SELECT 1',
  'ALTER TABLE `resource` ADD COLUMN `bind_id` BIGINT NULL AFTER `bind_type`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post' AND COLUMN_NAME = 'shared_path_id'
  ),
  'SELECT 1',
  'ALTER TABLE `post` ADD COLUMN `shared_path_id` BIGINT NULL AFTER `type`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'user_browse_history'
      AND COLUMN_NAME = 'route_url'
      AND CHARACTER_MAXIMUM_LENGTH >= 2048
  ),
  'SELECT 1',
  'ALTER TABLE `user_browse_history` MODIFY COLUMN `route_url` VARCHAR(2048) DEFAULT NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'book'
      AND COLUMN_NAME = 'cover_images'
      AND CHARACTER_MAXIMUM_LENGTH >= 2048
  ),
  'SELECT 1',
  'ALTER TABLE `book` MODIFY COLUMN `cover_images` VARCHAR(2048) DEFAULT NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
