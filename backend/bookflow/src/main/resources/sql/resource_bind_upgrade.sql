SET @db = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'resource' AND COLUMN_NAME = 'bind_type'
  ),
  'SELECT 1',
  'ALTER TABLE `resource` ADD COLUMN `bind_type` VARCHAR(32) NOT NULL DEFAULT ''none'' AFTER `book_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'resource' AND COLUMN_NAME = 'bind_id'
  ),
  'SELECT 1',
  'ALTER TABLE `resource` ADD COLUMN `bind_id` BIGINT NULL AFTER `bind_type`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
