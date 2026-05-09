SET @db = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_profile' AND COLUMN_NAME = 'auth_status'
  ),
  'SELECT 1',
  'ALTER TABLE `user_profile` ADD COLUMN `auth_status` INT DEFAULT 0 COMMENT ''认证状态：0未认证 1待审核 2已认证 3已驳回'' AFTER `department`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_profile' AND COLUMN_NAME = 'student_card_image_url'
  ),
  'SELECT 1',
  'ALTER TABLE `user_profile` ADD COLUMN `student_card_image_url` VARCHAR(255) NULL COMMENT ''学生证图片地址'' AFTER `intro`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_profile' AND COLUMN_NAME = 'verify_type'
  ),
  'SELECT 1',
  'ALTER TABLE `user_profile` ADD COLUMN `verify_type` VARCHAR(32) NULL COMMENT ''认证方式：student_card / edu_email'' AFTER `student_card_image_url`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_profile' AND COLUMN_NAME = 'verify_submit_time'
  ),
  'SELECT 1',
  'ALTER TABLE `user_profile` ADD COLUMN `verify_submit_time` DATETIME NULL COMMENT ''认证提交时间'' AFTER `verify_type`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_profile' AND COLUMN_NAME = 'audit_remark'
  ),
  'SELECT 1',
  'ALTER TABLE `user_profile` ADD COLUMN `audit_remark` VARCHAR(255) NULL COMMENT ''审核备注'' AFTER `verify_submit_time`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
