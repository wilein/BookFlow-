CREATE TABLE IF NOT EXISTS `user_path_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `path_id` BIGINT NOT NULL,
  `progress_percent` INT DEFAULT 0,
  `completed_count` INT DEFAULT 0,
  `started_at` DATETIME DEFAULT NULL,
  `last_learn_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_path` (`user_id`, `path_id`),
  KEY `idx_user_path_progress_path` (`path_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_path_node_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `path_id` BIGINT NOT NULL,
  `node_id` BIGINT NOT NULL,
  `completed` TINYINT DEFAULT 0,
  `completed_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_path_node` (`user_id`, `path_id`, `node_id`),
  KEY `idx_user_path_node_progress_path` (`path_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
