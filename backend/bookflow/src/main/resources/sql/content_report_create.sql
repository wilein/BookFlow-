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
