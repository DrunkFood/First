-- =====================================================================
-- Ele AI Tender 生产部署前清理测试数据脚本（MySQL）
-- 生成日期: 2026-06-28
--
-- 默认策略:
--   1. 清理业务测试数据：项目、需求、评审项、检测记录、AI任务、AI调用日志、消息、业务过程文件记录。
--   2. 保留生产基础配置：admin账号、角色、菜单、系统参数、模型配置、模型路由、模板管理、政策文件管理、知识库文档、接入系统。
--   3. 默认最后执行 ROLLBACK，仅用于预演；确认行数后，把最后一行 ROLLBACK 改成 COMMIT 再正式执行。
--
-- 执行前建议:
--   1. 停止 core / ai / file / support 服务，避免清理过程中产生新数据。
--   2. 先备份数据库，例如:
--      mysqldump -h<host> -P<port> -u<user> -p --single-transaction ele_ai_tender > ele_ai_tender_backup.sql
--   3. 先执行本脚本预演，确认“清理后预期剩余行数”符合预期，再改 COMMIT。
--
-- 注意:
--   1. 本脚本使用 DELETE，不使用 TRUNCATE，便于事务回滚。
--   2. DELETE 不会重置自增ID；如需重置，见脚本末尾“可选自增重置 SQL”，需单独执行。
--   3. ai_content_feedback 是代码里使用、但 init.sql 未建表的反馈表；脚本会检测存在性，存在则清理，不存在则跳过。
-- =====================================================================

USE `ele_ai_tender`;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
SET @has_ai_content_feedback = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_content_feedback'
);

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 1. 收集业务数据引用的文件ID，后续只删除这些业务文件，避免误删模板/系统政策/版本包文件。
-- ---------------------------------------------------------------------
CREATE TEMPORARY TABLE IF NOT EXISTS `cleanup_file_ids` (
    `id` BIGINT NOT NULL PRIMARY KEY
) ENGINE = MEMORY;

DELETE FROM `cleanup_file_ids`;

INSERT IGNORE INTO `cleanup_file_ids` (`id`)
SELECT file_id
FROM (
    SELECT `matched_file_id` AS file_id FROM `tb_project` WHERE `matched_file_id` IS NOT NULL
    UNION ALL
    SELECT `uploaded_file_id` AS file_id FROM `tb_project` WHERE `uploaded_file_id` IS NOT NULL
    UNION ALL
    SELECT `generated_file_id` AS file_id FROM `tb_project` WHERE `generated_file_id` IS NOT NULL
    UNION ALL
    SELECT `matched_file_id` AS file_id FROM `tb_requirement` WHERE `matched_file_id` IS NOT NULL
    UNION ALL
    SELECT `uploaded_file_id` AS file_id FROM `tb_requirement` WHERE `uploaded_file_id` IS NOT NULL
) f
WHERE file_id IS NOT NULL;

-- ai_task.file_ids 是逗号分隔的文件ID列表。MySQL 8 支持 JSON_TABLE。
INSERT IGNORE INTO `cleanup_file_ids` (`id`)
SELECT jt.file_id
FROM `ai_task` t
JOIN JSON_TABLE(
    CONCAT('["', REPLACE(REPLACE(t.`file_ids`, ' ', ''), ',', '","'), '"]'),
    '$[*]' COLUMNS (`file_id` BIGINT PATH '$')
) jt
WHERE t.`file_ids` IS NOT NULL
  AND t.`file_ids` <> '';

-- ---------------------------------------------------------------------
-- 2. 清理前行数预检查。
-- ---------------------------------------------------------------------
SELECT 'BEFORE' AS stage, 'tb_project' AS table_name, COUNT(*) AS row_count FROM `tb_project`
UNION ALL SELECT 'BEFORE', 'tb_requirement', COUNT(*) FROM `tb_requirement`
UNION ALL SELECT 'BEFORE', 'tb_project_template', COUNT(*) FROM `tb_project_template`
UNION ALL SELECT 'BEFORE', 'tb_project_review_item', COUNT(*) FROM `tb_project_review_item`
UNION ALL SELECT 'BEFORE', 'tb_project_version', COUNT(*) FROM `tb_project_version`
UNION ALL SELECT 'BEFORE', 'tb_detection_record', COUNT(*) FROM `tb_detection_record`
UNION ALL SELECT 'BEFORE', 'ai_task', COUNT(*) FROM `ai_task`
UNION ALL SELECT 'BEFORE', 'ai_response_log', COUNT(*) FROM `ai_response_log`
UNION ALL SELECT 'BEFORE', 'sup_message', COUNT(*) FROM `sup_message`
UNION ALL SELECT 'BEFORE', 'sup_sms_code', COUNT(*) FROM `sup_sms_code`
UNION ALL SELECT 'BEFORE', 'sup_operation_log', COUNT(*) FROM `sup_operation_log`
UNION ALL SELECT 'BEFORE', 'sup_access_log', COUNT(*) FROM `sup_access_log`
UNION ALL SELECT 'BEFORE', 'file_info_to_delete', COUNT(*) FROM `cleanup_file_ids`;

SET @sql = IF(
    @has_ai_content_feedback > 0,
    'SELECT ''BEFORE'' AS stage, ''ai_content_feedback'' AS table_name, COUNT(*) AS row_count FROM `ai_content_feedback`',
    'SELECT ''BEFORE'' AS stage, ''ai_content_feedback'' AS table_name, 0 AS row_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. 删除业务测试数据。
-- 删除顺序按业务依赖从子表/日志表到主表。
-- ---------------------------------------------------------------------

-- AI反馈、AI响应。
SET @sql = IF(
    @has_ai_content_feedback > 0,
    'DELETE FROM `ai_content_feedback`',
    'SELECT ''SKIP_DELETE'' AS stage, ''ai_content_feedback'' AS table_name, ''table_not_exists'' AS reason'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DELETE FROM `ai_response_log`;

-- 检测记录引用AI任务，先删检测记录，再删AI任务。
DELETE FROM `tb_detection_record`;
DELETE FROM `ai_task`;

-- 评审项、版本、模板快照。
DELETE FROM `tb_project_review_item`;
DELETE FROM `tb_project_version`;
DELETE FROM `tb_project_template`;

-- 项目和业务需求。
DELETE FROM `tb_project`;
DELETE FROM `tb_requirement`;

-- 消息与验证码。菜单、角色、系统参数等基础配置保留。
DELETE FROM `sup_message`;
DELETE FROM `sup_sms_code`;

-- 测试阶段访问/操作日志。若生产要求保留审计日志，可注释这两行。
DELETE FROM `sup_operation_log`;
DELETE FROM `sup_access_log`;

-- 用户账号只保留 admin。角色、菜单、角色-菜单权限配置保留。
DELETE ur
FROM `sup_user_role` ur
JOIN `sup_user` u ON u.`id` = ur.`user_id`
WHERE u.`username` <> 'admin';

DELETE FROM `sup_user`
WHERE `username` <> 'admin';

-- 删除业务文件元数据，但保留仍被模板、系统政策、知识库、版本包引用的文件。
DELETE fi
FROM `file_info` fi
LEFT JOIN `sup_template` st
       ON st.`file_id` = fi.`id` AND st.`is_delete` = 0
LEFT JOIN `sup_policy_file` spf
       ON spf.`file_id` = fi.`id` AND spf.`is_delete` = 0
LEFT JOIN `tb_policy_file` tpf
       ON tpf.`file_id` = fi.`id` AND tpf.`is_delete` = 0
LEFT JOIN `ai_knowledge_document` akd
       ON akd.`file_id` = fi.`id` AND akd.`is_delete` = 0
LEFT JOIN `sup_main_version` smv
       ON smv.`file_id` = fi.`id` AND smv.`is_delete` = 0
LEFT JOIN `sup_plugin_version` spv
       ON spv.`file_id` = fi.`id` AND spv.`is_delete` = 0
WHERE (
        fi.`id` IN (SELECT `id` FROM `cleanup_file_ids`)
        OR fi.`biz_type` IN ('requirement', 'generated', 'detection-fixed')
      )
  AND st.`id` IS NULL
  AND spf.`id` IS NULL
  AND tpf.`id` IS NULL
  AND akd.`id` IS NULL
  AND smv.`id` IS NULL
  AND spv.`id` IS NULL;

-- ---------------------------------------------------------------------
-- 4. 清理后行数检查。正式 COMMIT 前请确认这些业务表为 0。
-- ---------------------------------------------------------------------
SELECT 'AFTER_DELETE_BEFORE_COMMIT' AS stage, 'tb_project' AS table_name, COUNT(*) AS row_count FROM `tb_project`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'tb_requirement', COUNT(*) FROM `tb_requirement`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'tb_project_template', COUNT(*) FROM `tb_project_template`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'tb_project_review_item', COUNT(*) FROM `tb_project_review_item`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'tb_project_version', COUNT(*) FROM `tb_project_version`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'tb_detection_record', COUNT(*) FROM `tb_detection_record`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'ai_task', COUNT(*) FROM `ai_task`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'ai_response_log', COUNT(*) FROM `ai_response_log`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'sup_message', COUNT(*) FROM `sup_message`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'sup_sms_code', COUNT(*) FROM `sup_sms_code`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'sup_operation_log', COUNT(*) FROM `sup_operation_log`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'sup_access_log', COUNT(*) FROM `sup_access_log`
UNION ALL SELECT 'AFTER_DELETE_BEFORE_COMMIT', 'non_admin_user', COUNT(*) FROM `sup_user` WHERE `username` <> 'admin';

SET @sql = IF(
    @has_ai_content_feedback > 0,
    'SELECT ''AFTER_DELETE_BEFORE_COMMIT'' AS stage, ''ai_content_feedback'' AS table_name, COUNT(*) AS row_count FROM `ai_content_feedback`',
    'SELECT ''AFTER_DELETE_BEFORE_COMMIT'' AS stage, ''ai_content_feedback'' AS table_name, 0 AS row_count'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 基础配置保留检查。
SELECT 'KEEP_CHECK' AS stage, 'sup_user_admin' AS table_name, COUNT(*) AS row_count
FROM `sup_user`
WHERE `username` = 'admin'
UNION ALL SELECT 'KEEP_CHECK', 'sup_role', COUNT(*) FROM `sup_role`
UNION ALL SELECT 'KEEP_CHECK', 'sup_menu', COUNT(*) FROM `sup_menu`
UNION ALL SELECT 'KEEP_CHECK', 'sup_sys_parameter', COUNT(*) FROM `sup_sys_parameter`
UNION ALL SELECT 'KEEP_CHECK', 'sup_model_config', COUNT(*) FROM `sup_model_config`
UNION ALL SELECT 'KEEP_CHECK', 'sup_model_route_rule', COUNT(*) FROM `sup_model_route_rule`
UNION ALL SELECT 'KEEP_CHECK', 'sup_template', COUNT(*) FROM `sup_template`
UNION ALL SELECT 'KEEP_CHECK', 'sup_policy_file', COUNT(*) FROM `sup_policy_file`
UNION ALL SELECT 'KEEP_CHECK', 'tb_policy_file', COUNT(*) FROM `tb_policy_file`
UNION ALL SELECT 'KEEP_CHECK', 'ai_knowledge_document', COUNT(*) FROM `ai_knowledge_document`;

-- ---------------------------------------------------------------------
-- 5. 默认预演，不真正提交。
--    正式清理时，把 ROLLBACK 改成 COMMIT。
-- ---------------------------------------------------------------------
ROLLBACK;
-- COMMIT;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

-- ---------------------------------------------------------------------
-- 6. 可选：正式 COMMIT 后，如需重置自增ID，再单独执行以下语句。
--    注意：ALTER TABLE 会隐式提交，不能放在预演事务里执行。
-- ---------------------------------------------------------------------
-- ALTER TABLE `tb_project` AUTO_INCREMENT = 1;
-- ALTER TABLE `tb_requirement` AUTO_INCREMENT = 1;
-- ALTER TABLE `tb_project_template` AUTO_INCREMENT = 1;
-- ALTER TABLE `tb_project_review_item` AUTO_INCREMENT = 1;
-- ALTER TABLE `tb_project_version` AUTO_INCREMENT = 1;
-- ALTER TABLE `tb_detection_record` AUTO_INCREMENT = 1;
-- ALTER TABLE `ai_task` AUTO_INCREMENT = 1;
-- ALTER TABLE `ai_response_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `ai_content_feedback` AUTO_INCREMENT = 1; -- 仅当该表存在时执行
-- ALTER TABLE `sup_message` AUTO_INCREMENT = 1;
-- ALTER TABLE `sup_sms_code` AUTO_INCREMENT = 1;
-- ALTER TABLE `sup_operation_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `sup_access_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `file_info` AUTO_INCREMENT = 1;

-- ---------------------------------------------------------------------
-- 7. 可选增强清理：如确认知识库文档也是测试数据，可由 DBA 单独评估后清理。
--    政策文件管理和模板管理数据本脚本固定保留，不在清测范围内。
-- ---------------------------------------------------------------------
-- DELETE FROM `ai_knowledge_document`;
