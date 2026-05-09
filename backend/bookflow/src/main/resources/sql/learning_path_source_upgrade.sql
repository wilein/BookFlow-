SET @db = DATABASE();

SET @sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'learning_path' AND COLUMN_NAME = 'source_path_id'
  ),
  'SELECT 1',
  'ALTER TABLE `learning_path` ADD COLUMN `source_path_id` BIGINT NULL AFTER `book_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
