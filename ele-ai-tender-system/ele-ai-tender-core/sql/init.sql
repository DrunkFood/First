-- =========================================
-- Ele AI Tender 核心业务模块数据库初始化脚本
-- 数据库: ele_ai_tender (db=6)
-- 表前缀: ai_
-- =========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ele_ai_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_ai_tender`;

-- 1. AI编制项目表
CREATE TABLE IF NOT EXISTS `ai_project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `project_code` VARCHAR(50) NOT NULL COMMENT '项目编号',
    `project_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
    `project_category` VARCHAR(30) NOT NULL COMMENT '项目类别: LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT',
    `project_type` VARCHAR(30) NOT NULL COMMENT '项目类型: ENGINEERING/GOODS/SERVICE',
    `service_sub_type` VARCHAR(50) DEFAULT NULL COMMENT '服务子类型: PROPERTY/IT_SERVICE/CONSULTING/MAINTENANCE',
    `budget` DECIMAL(15,2) DEFAULT NULL COMMENT '预算金额(万元)',
    `review_type` VARCHAR(30) DEFAULT NULL COMMENT '评审类型: MANUAL/INTELLIGENT',
    `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '项目状态: DRAFT/IN_PROGRESS/PENDING_DETECTION/DETECTING/DETECTION_PASSED/DETECTION_FAILED/DETECTION_SKIPPED/PUBLISHED/ARCHIVED/CANCELLED',
    `template_id` BIGINT DEFAULT NULL COMMENT '使用的模板ID',
    `requirement_id` BIGINT DEFAULT NULL COMMENT '关联的业务需求ID',
    `requirement_source` VARCHAR(30) DEFAULT NULL COMMENT '需求来源: REFERENCE/AI_GENERATED',
    `requirement_content` TEXT DEFAULT NULL COMMENT '招标需求内容',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_code` (`project_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_category` (`project_category`),
    INDEX `idx_create_id` (`create_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI编制项目表';

-- 2. AI编制项目版本表
CREATE TABLE IF NOT EXISTS `ai_project_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `version_no` INT NOT NULL COMMENT '版本号',
    `content_snapshot` JSON DEFAULT NULL COMMENT '内容快照',
    `change_description` VARCHAR(500) DEFAULT NULL COMMENT '变更说明',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_project_version` (`project_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI编制项目版本表';

-- 3. 业务需求表
CREATE TABLE IF NOT EXISTS `ai_requirement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '需求ID',
    `requirement_name` VARCHAR(100) NOT NULL COMMENT '需求名称',
    `project_category` VARCHAR(30) NOT NULL COMMENT '项目类别',
    `project_type` VARCHAR(30) NOT NULL COMMENT '项目类型',
    `budget` DECIMAL(15,2) DEFAULT NULL COMMENT '预算价(万元)',
    `requirement_description` VARCHAR(500) DEFAULT NULL COMMENT '需求描述',
    `match_mode` VARCHAR(30) DEFAULT NULL COMMENT '匹配模式: AUTO_MATCH/MANUAL_SELECT/UPLOAD',
    `matched_file_id` BIGINT DEFAULT NULL COMMENT '匹配的历史文件ID',
    `matched_similarity` DECIMAL(5,2) DEFAULT NULL COMMENT '匹配度百分比',
    `uploaded_file_id` BIGINT DEFAULT NULL COMMENT '上传的文件ID',
    `project_id` BIGINT DEFAULT NULL COMMENT '关联的项目ID',
    `content` TEXT DEFAULT NULL COMMENT '业务需求内容',
    `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_id` (`create_id`),
    INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务需求表';

-- 4. 招标文件模板表
CREATE TABLE IF NOT EXISTS `ai_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `project_category` VARCHAR(30) NOT NULL COMMENT '适用项目类别',
    `project_type` VARCHAR(30) NOT NULL COMMENT '适用项目类型',
    `content` LONGTEXT NOT NULL COMMENT '模板内容(Markdown格式)',
    `structure_definition` JSON DEFAULT NULL COMMENT '模板结构定义',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认模板: 0-否, 1-是',
    `status` VARCHAR(30) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template_code` (`template_code`),
    INDEX `idx_category_type` (`project_category`, `project_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招标文件模板表';

-- 5. 评审项表（三级嵌套结构）
CREATE TABLE IF NOT EXISTS `ai_review_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评审项ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID, 0表示顶级节点',
    `level` INT NOT NULL DEFAULT 1 COMMENT '层级: 1/2/3',
    `item_name` VARCHAR(200) NOT NULL COMMENT '评审项名称',
    `item_content` TEXT DEFAULT NULL COMMENT '评审项内容',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
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
    INDEX `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评审项表';
