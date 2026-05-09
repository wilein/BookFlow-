CREATE TABLE IF NOT EXISTS `post_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  `action_type` TINYINT NOT NULL COMMENT '1-点赞 2-收藏',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post_action` (`user_id`, `post_id`, `action_type`),
  KEY `idx_post_action_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
