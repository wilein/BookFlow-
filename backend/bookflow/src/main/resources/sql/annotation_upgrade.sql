ALTER TABLE `annotation`
  ADD COLUMN `position_text` VARCHAR(120) NULL DEFAULT NULL COMMENT '批注位置描述' AFTER `content`,
  ADD COLUMN `image_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '批注图片' AFTER `position_text`;
