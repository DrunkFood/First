-- =====================================================
-- 招标文件AI编制工具 - AI系统全量初始化脚本
-- 数据库: ele_ai_tender (MySQL 8.4+, db=6)
-- 说明: 包含AI模块全部建表语句 + 初始数据
-- 执行顺序: support/init.sql → 本脚本 → core/init.sql
-- =====================================================

CREATE DATABASE IF NOT EXISTS `ele_ai_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_ai_tender`;

-- =============================================
-- 2. AI任务队列表
-- 实体: com.jy.eleaitender.common.entity.ai.AiTask
-- 说明: Core写入PENDING任务, AI轮询PROCESSING, CAS状态更新
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_task` (
                                         `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                                         `task_type`       VARCHAR(50)  NOT NULL COMMENT '任务类型: REQUIREMENT_GENERATE/REVIEW_ITEM_GENERATE/DETECTION_SENSITIVE_WORD/DETECTION_TYPO/DETECTION_POLICY_REVIEW/DETECTION_FORMAT_CHECK/TEXT_OPTIMIZE',
                                         `project_id`      BIGINT       DEFAULT NULL COMMENT '关联项目ID',
                                         `biz_id`          BIGINT       DEFAULT NULL COMMENT '关联业务ID(需求ID/项目ID/检测记录ID)',
                                         `biz_type`        VARCHAR(30)  DEFAULT NULL COMMENT '业务类型: REQUIREMENT/PROJECT/DETECTION',
                                         `request_params`  TEXT         DEFAULT NULL COMMENT '请求参数(JSON)',
                                         `file_ids`        VARCHAR(500) DEFAULT NULL COMMENT '关联文件ID列表(逗号分隔)',
                                         `status`          VARCHAR(30)  NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING/PROCESSING/COMPLETED/FAILED/AI_UNAVAILABLE/SKIPPED',
                                         `result`          LONGTEXT     DEFAULT NULL COMMENT '执行结果(JSON)',
                                         `error_msg`       VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
                                         `retry_count`     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
                                         `max_retry`       INT          NOT NULL DEFAULT 3 COMMENT '最大重试次数',
                                         `started_at`      DATETIME     DEFAULT NULL COMMENT 'AI开始处理时间',
                                         `completed_at`    DATETIME     DEFAULT NULL COMMENT '完成时间',
                                         `timeout_minutes` INT          DEFAULT NULL COMMENT '超时时间(分钟), 默认10',
                                         `create_time`     DATETIME     NOT NULL COMMENT '创建时间',
                                         `create_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
                                         `create_name`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
                                         `modify_time`     DATETIME     NOT NULL COMMENT '修改时间',
                                         `modify_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
                                         `modify_name`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
                                         `ver`             INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
                                         `is_delete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
                                         PRIMARY KEY (`id`),
                                         INDEX `idx_status` (`status`),
                                         INDEX `idx_project` (`project_id`),
                                         INDEX `idx_biz` (`biz_id`, `biz_type`),
                                         INDEX `idx_task_type_status` (`task_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI任务队列表';

-- =====================================================
-- 3. 知识库文档表 (ai_knowledge_document)
-- =====================================================
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `doc_name`          VARCHAR(200) NOT NULL COMMENT '文档名称',
    `doc_category`      VARCHAR(50)  DEFAULT NULL COMMENT '文档类别: POLICY(政策文件)/HISTORY_TEMPLATE(历史模板)/STANDARD(标准规范)/OTHER',
    `file_id`           BIGINT       NOT NULL COMMENT '文件ID(关联file_info表)',
    `file_type`         VARCHAR(20)  DEFAULT NULL COMMENT '文件类型: DOC/DOCX/PDF/TXT/MD',
    `content`           LONGTEXT     DEFAULT NULL COMMENT '文档文本内容(解析后的纯文本)',
    `vector_collection` VARCHAR(100) DEFAULT NULL COMMENT '向量集合名称(Milvus collection)',
    `vector_ids`        JSON         DEFAULT NULL COMMENT '向量ID列表(关联Milvus中的向量)',
    `status`            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ARCHIVED/PROCESSING/FAILED',
    `create_time`       DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`       DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`               INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`doc_category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- =====================================================
-- 4. AI响应记录表 (ai_response_log)
--    记录每次AI模型调用的详细信息：请求内容、响应内容、token消耗等
-- =====================================================
CREATE TABLE IF NOT EXISTS `ai_response_log` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `model`             VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '模型名称',
    `role`              VARCHAR(16)  NOT NULL COMMENT '对话角色: GENERATION/OPTIMIZATION/DETECTION/CHAT',
    `messages`          LONGTEXT     NOT NULL COMMENT '对话内容messages(JSON)',
    `content`           LONGTEXT     NOT NULL COMMENT 'AI响应内容',
    `finish_reason`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '完成原因: stop/length/tool_calls等',
    `prompt_tokens`     INT          NOT NULL DEFAULT 0 COMMENT '包含历史问题的总tokens大小',
    `completion_tokens` INT          NOT NULL DEFAULT 0 COMMENT '回答的tokens大小',
    `total_tokens`      INT          NOT NULL DEFAULT 0 COMMENT '本次交互计费的tokens大小',
    `task_id`           BIGINT       DEFAULT NULL COMMENT '关联AI任务ID(ai_task.id), 对话类调用为NULL',
    `conversation_id`   VARCHAR(64)  DEFAULT NULL COMMENT '对话ID, 发起AI聊天时随机生成的UUID',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID(任务类调用取ai_task.create_id)',
    `create_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`               INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_role` (`role`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_model` (`model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI响应记录表';

-- =====================================================
-- 验证: 查看已创建的表
-- =====================================================
SELECT 'AI系统初始化完成!' AS message;

SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'ele_ai_tender'
  AND TABLE_NAME IN (
    'sup_model_config', 'ai_task', 'ai_knowledge_document', 'ai_response_log'
  )
ORDER BY TABLE_NAME;
