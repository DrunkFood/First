-- =========================================
-- Ele AI Tender AI模块数据库初始化脚本
-- 数据库: ele_ai_tender (db=6)
-- 表前缀: ai_
-- =========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ele_ai_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_ai_tender`;

-- 1. 知识库文档表
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `doc_name` VARCHAR(200) NOT NULL COMMENT '文档名称',
    `doc_category` VARCHAR(50) DEFAULT NULL COMMENT '文档类别: POLICY/HISTORY_TEMPLATE/STANDARD',
    `file_id` BIGINT NOT NULL COMMENT '文件ID(关联文件服务)',
    `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型: DOC/DOCX/PDF',
    `content` TEXT DEFAULT NULL COMMENT '文档文本内容',
    `vector_collection` VARCHAR(100) DEFAULT NULL COMMENT '向量集合名称',
    `vector_ids` JSON DEFAULT NULL COMMENT '向量ID列表',
    `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ARCHIVED',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`doc_category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档表';

-- 2. 检测记录表
CREATE TABLE IF NOT EXISTS `ai_detection_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '检测记录ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `detection_type` VARCHAR(50) DEFAULT NULL COMMENT '检测类型: FAIRNESS/COMPLIANCE/TYPO/SENSITIVE_WORD',
    `content_snapshot` TEXT DEFAULT NULL COMMENT '检测内容快照',
    `result` JSON DEFAULT NULL COMMENT '检测结果(JSON格式)',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/PASSED/FAILED/SKIPPED',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_project` (`project_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检测记录表';

-- 3. AI模型配置表
CREATE TABLE IF NOT EXISTS `ai_model_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `model_type` VARCHAR(30) NOT NULL COMMENT '模型类型: LOCAL/CLOUD/PRIVATE',
    `api_endpoint` VARCHAR(500) DEFAULT NULL COMMENT 'API端点',
    `api_key` VARCHAR(500) DEFAULT NULL COMMENT 'API密钥(加密存储)',
    `model_params` JSON DEFAULT NULL COMMENT '模型参数配置',
    `usage_scenario` VARCHAR(50) DEFAULT NULL COMMENT '使用场景: GENERATION/OPTIMIZATION/DETECTION',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `token_usage` BIGINT NOT NULL DEFAULT 0 COMMENT 'Token使用量',
    `cost` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计费用',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI模型配置表';
