SET @db = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post' AND COLUMN_NAME = 'shared_path_id'
  ),
  'SELECT 1',
  'ALTER TABLE `post` ADD COLUMN `shared_path_id` BIGINT NULL AFTER `type`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
