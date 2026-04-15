-- =====================================================
-- 招标文件AI编制工具 - AI系统全量初始化脚本
-- 数据库: ele_ai_tender (MySQL 8.4+, db=6)
-- 说明: 包含AI模块全部建表语句 + 初始数据
-- 执行顺序: support/init.sql → 本脚本 → core/init.sql
-- =====================================================

CREATE DATABASE IF NOT EXISTS `ele_ai_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_ai_tender`;

-- =====================================================
-- 1. AI模型配置表 (ai_model_config)
--    支撑中心管理，AI模块读取（只读缓存）
-- =====================================================
CREATE TABLE IF NOT EXISTS `ai_model_config` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `model_name`      VARCHAR(100) NOT NULL COMMENT '模型名称',
    `model_type`      VARCHAR(20)  NOT NULL COMMENT '模型类型: LOCAL(本地微调)/CLOUD(云端大模型)/PRIVATE(私有化部署)',
    `api_endpoint`    VARCHAR(500) DEFAULT NULL COMMENT 'API端点地址',
    `api_key`         VARCHAR(500) DEFAULT NULL COMMENT 'API密钥(AES加密存储)',
    `model_params`    JSON         DEFAULT NULL COMMENT '模型参数(temperature/maxTokens/topP/model等)',
    `usage_scenario`  VARCHAR(50)  DEFAULT NULL COMMENT '使用场景: GENERATION/OPTIMIZATION/DETECTION',
    `is_active`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用: 0-停用, 1-启用',
    `token_usage`     BIGINT       NOT NULL DEFAULT 0 COMMENT 'Token使用量(累计)',
    `cost`            DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计费用(元)',
    `create_time`     DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`     DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`             INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_model_type` (`model_type`),
    INDEX `idx_usage_scenario` (`usage_scenario`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- 初始化默认模型配置
INSERT INTO `ai_model_config` (`model_name`, `model_type`, `api_endpoint`, `api_key`, `model_params`, `usage_scenario`, `is_active`, `create_time`, `modify_time`) VALUES
('DeepSeek-Chat', 'CLOUD', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.7, "maxTokens": 4096, "topP": 0.9, "model": "deepseek-chat"}', 'GENERATION', 1, NOW(), NOW()),
('DeepSeek-Chat', 'CLOUD', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.3, "maxTokens": 2048, "topP": 0.85, "model": "deepseek-chat"}', 'OPTIMIZATION', 1, NOW(), NOW()),
('DeepSeek-Chat', 'CLOUD', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.1, "maxTokens": 4096, "topP": 0.8, "model": "deepseek-chat"}', 'DETECTION', 1, NOW(), NOW());

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
-- 验证: 查看已创建的表
-- =====================================================
SELECT 'AI系统初始化完成!' AS message;

SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'ele_ai_tender'
  AND TABLE_NAME IN (
    'ai_model_config', 'ai_task', 'ai_knowledge_document'
  )
ORDER BY TABLE_NAME;
