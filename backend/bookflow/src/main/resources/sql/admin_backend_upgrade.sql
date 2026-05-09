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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `admin_user`
(`username`, `password_hash`, `real_name`, `role`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT
  'admin',
  '031ece227091c836194724e435a86ca9988b35f6810a94a92ae656b19d1a04e8',
  '超级管理员',
  'super_admin',
  1,
  NOW(),
  NOW(),
  0
WHERE NOT EXISTS (
  SELECT 1 FROM `admin_user` WHERE `is_deleted` = 0
);
