CREATE TABLE IF NOT EXISTS `annotation_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `annotation_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_annotation_like_user` (`annotation_id`, `user_id`),
  KEY `idx_annotation_like_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
