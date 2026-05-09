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
