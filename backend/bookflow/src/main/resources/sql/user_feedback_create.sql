CREATE TABLE IF NOT EXISTS `user_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `feedback_type` VARCHAR(32) NOT NULL,
  `content` VARCHAR(2000) NOT NULL,
  `contact` VARCHAR(100) DEFAULT NULL,
  `page_path` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 0 COMMENT '0-未处理',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_feedback_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈';
