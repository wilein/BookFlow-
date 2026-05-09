CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `book_id` BIGINT DEFAULT NULL,
  `buyer_id` BIGINT DEFAULT NULL,
  `seller_id` BIGINT DEFAULT NULL,
  `last_message` VARCHAR(500) DEFAULT NULL,
  `last_message_time` DATETIME DEFAULT NULL,
  `unread_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_chat_session_book` (`book_id`),
  KEY `idx_chat_session_buyer` (`buyer_id`),
  KEY `idx_chat_session_seller` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `sender_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `is_read` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_chat_message_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
