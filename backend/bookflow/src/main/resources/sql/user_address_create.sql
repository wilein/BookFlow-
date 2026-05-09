CREATE TABLE IF NOT EXISTS `user_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'user id',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT 'receiver name',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT 'receiver phone',
    `province` VARCHAR(50) NOT NULL COMMENT 'province',
    `city` VARCHAR(50) NOT NULL COMMENT 'city',
    `district` VARCHAR(50) NOT NULL COMMENT 'district',
    `detail_address` VARCHAR(200) NOT NULL COMMENT 'detail address',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '0-no 1-yes',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_address_user_id` (`user_id`) USING BTREE,
    KEY `idx_user_address_default` (`user_id`, `is_default`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user address table';
