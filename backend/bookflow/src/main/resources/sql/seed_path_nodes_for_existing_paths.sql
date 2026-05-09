-- 为所有现有学习路径补齐可学习节点和节点资源。
-- 可重复执行：每条路径至少补齐 6 个节点，资源按 bind_id + title 去重。

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_bookflow_node_templates (
  order_num INT PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  estimated_minutes INT NOT NULL
) ENGINE=Memory;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_bookflow_node_resources (
  id INT PRIMARY KEY,
  order_num INT NOT NULL,
  title_pattern VARCHAR(160) NOT NULL,
  type TINYINT NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_format VARCHAR(32) NOT NULL,
  description VARCHAR(500) NOT NULL
) ENGINE=Memory;

TRUNCATE TABLE tmp_bookflow_node_templates;
TRUNCATE TABLE tmp_bookflow_node_resources;

INSERT INTO tmp_bookflow_node_templates (order_num, title, description, estimated_minutes) VALUES
(1, '学习目标与环境准备', '明确本路径的学习目标、先修基础、最终成果和每天投入时间；准备教材、笔记模板、练习环境和资料目录。', 45),
(2, '核心概念精读', '按章节梳理核心概念、术语和关键模型，完成第一轮通读；把不理解的定义、公式、代码片段或章节问题记录到疑问清单。', 120),
(3, '重点案例拆解', '选取书中或课程中的重点案例，拆解背景、输入输出、步骤、结论和易错点；把完整推导、流程图或代码过程补充到笔记中。', 150),
(4, '练习与项目实践', '完成配套练习或一个小型实践任务，用题目、代码、项目或问答输出验证是否真正掌握本阶段内容，并记录完成结果。', 180),
(5, '错题复盘与知识卡片', '回看前面记录的问题、错题和卡点，补齐薄弱知识点；整理可复用的知识卡片、错题原因和下一次复习提醒。', 75),
(6, '输出总结与进阶计划', '整理完整学习总结，沉淀书摘、批注、问答或项目说明；根据掌握情况规划下一条进阶路径或复习计划。', 60);

INSERT INTO tmp_bookflow_node_resources (id, order_num, title_pattern, type, file_url, file_format, description) VALUES
(1, 1, '{path} - 目标与计划模板', 1, 'https://www.coursera.org/articles/study-plan', 'link', '用于填写学习目标、每日安排、验收标准和复盘问题的计划模板。'),
(2, 2, '核心概念学习法视频', 4, 'https://www.bilibili.com/', 'video', '用视频方式演示如何通过概念图、问题清单和例题把核心章节读透。'),
(3, 2, '{path} - 核心概念笔记模板', 3, 'https://www.notion.so/templates', 'link', '用于整理术语、定义、公式、代码片段和疑问清单的笔记模板入口。'),
(4, 3, '{path} - 案例拆解清单', 1, 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf', 'pdf', '按背景、步骤、关键结论、易错点四部分拆解案例的 PDF 清单。'),
(5, 4, '{path} - 阶段练习题单', 2, 'https://github.com/practical-tutorials/project-based-learning', 'link', '包含基础题、综合题和实践任务，用于检查阶段学习效果。'),
(6, 4, '{path} - 实践记录表', 3, 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf', 'pdf', '用于记录练习结果、错误原因、修正过程和最终结论。'),
(7, 5, '错题复盘与知识卡片模板', 3, 'https://www.coursera.org/articles/how-to-take-notes', 'link', '记录错题原因、薄弱点、复习时间和知识卡片的复盘方法。'),
(8, 6, '{path} - 结课总结示例', 5, 'https://github.com/practical-tutorials/project-based-learning', 'link', '示例展示如何写学习总结、沉淀书摘批注并规划下一步。');

UPDATE path_node
SET resource_ids = JSON_ARRAY()
WHERE resource_ids IS NULL OR CAST(resource_ids AS CHAR) = '';

INSERT INTO path_node (
  path_id, parent_id, title, description, order_num, estimated_minutes,
  resource_ids, create_time, update_time, is_deleted
)
SELECT
  p.id,
  0,
  t.title,
  CONCAT('围绕《', COALESCE(p.title, '学习路径'), '》', t.description),
  t.order_num,
  t.estimated_minutes,
  JSON_ARRAY(),
  NOW(),
  NOW(),
  0
FROM learning_path p
JOIN tmp_bookflow_node_templates t
WHERE COALESCE(p.is_deleted, 0) = 0
  AND NOT EXISTS (
    SELECT 1
    FROM path_node n
    WHERE n.path_id = p.id
      AND n.order_num = t.order_num
      AND COALESCE(n.is_deleted, 0) = 0
  );

UPDATE path_node n
JOIN learning_path p ON p.id = n.path_id AND COALESCE(p.is_deleted, 0) = 0
JOIN tmp_bookflow_node_templates t ON t.order_num = n.order_num
SET
  n.description = CONCAT('围绕《', COALESCE(p.title, '学习路径'), '》', t.description),
  n.estimated_minutes = CASE
    WHEN n.estimated_minutes IS NULL OR n.estimated_minutes <= 0 THEN t.estimated_minutes
    ELSE n.estimated_minutes
  END,
  n.update_time = NOW()
WHERE COALESCE(n.is_deleted, 0) = 0
  AND CHAR_LENGTH(COALESCE(n.description, '')) < 24;

INSERT INTO `resource` (
  user_id, book_id, bind_type, bind_id, title, type, file_url, file_size,
  file_format, download_count, description, visibility, create_time, update_time, is_deleted
)
SELECT
  p.user_id,
  NULL,
  'pathNode',
  n.id,
  REPLACE(r.title_pattern, '{path}', COALESCE(p.title, '学习路径')),
  r.type,
  r.file_url,
  NULL,
  r.file_format,
  0,
  r.description,
  1,
  NOW(),
  NOW(),
  0
FROM learning_path p
JOIN path_node n ON n.path_id = p.id AND COALESCE(n.is_deleted, 0) = 0
JOIN tmp_bookflow_node_resources r ON r.order_num = n.order_num
WHERE COALESCE(p.is_deleted, 0) = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `resource` existing
    WHERE existing.bind_type = 'pathNode'
      AND existing.bind_id = n.id
      AND existing.title COLLATE utf8mb4_unicode_ci = REPLACE(r.title_pattern, '{path}', COALESCE(p.title, '学习路径')) COLLATE utf8mb4_unicode_ci
      AND COALESCE(existing.is_deleted, 0) = 0
  );

UPDATE path_node n
SET resource_ids = COALESCE((
  SELECT JSON_ARRAYAGG(r.id)
  FROM `resource` r
  WHERE r.bind_type = 'pathNode'
    AND r.bind_id = n.id
    AND COALESCE(r.is_deleted, 0) = 0
), JSON_ARRAY())
WHERE COALESCE(n.is_deleted, 0) = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_bookflow_node_resources;
DROP TEMPORARY TABLE IF EXISTS tmp_bookflow_node_templates;
