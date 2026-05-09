CREATE TABLE IF NOT EXISTS `user_browse_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `target_type` VARCHAR(32) NOT NULL COMMENT 'book/path',
  `target_id` BIGINT NOT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `sub_title` VARCHAR(200) DEFAULT NULL,
  `cover_url` VARCHAR(255) DEFAULT NULL,
  `route_url` VARCHAR(2048) DEFAULT NULL,
  `last_view_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_user_last_view` (`user_id`, `last_view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户浏览历史';
