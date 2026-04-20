-- =========================================
-- Ele AI Tender 编制中心 全量初始化脚本
-- 数据库: ele_ai_tender (db=6)
-- 表前缀: ai_
-- 生成时间: 2026-04-15
-- 覆盖模块: core + ai + common
-- =========================================
-- 说明:
--   本脚本为编制中心全量建表脚本，包含: ai_project / ai_project_version / ai_requirement / ai_review_item / ai_detection_record / ai_policy_file / ai_template
--   所有字段严格对应Java实体类定义
-- =========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ele_ai_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_ai_tender`;

-- =============================================
-- 1. AI编制项目表
-- 实体: com.jy.eleaitender.common.entity.core.AiProject
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_project` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `project_code`          VARCHAR(50)  NOT NULL COMMENT '项目编号',
    `project_name`          VARCHAR(100) NOT NULL COMMENT '项目名称',
    `project_category`      VARCHAR(30)  NOT NULL COMMENT '项目类别: LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT',
    `project_type`          VARCHAR(30)  NOT NULL COMMENT '项目类型: ENGINEERING/GOODS/SERVICE',
    `service_sub_type`      VARCHAR(50)  DEFAULT NULL COMMENT '服务子类型(当project_type=SERVICE时)',
    `budget`                DECIMAL(15,2) DEFAULT NULL COMMENT '预算金额(元)',
    `project_description`   VARCHAR(500) DEFAULT NULL COMMENT '项目基本情况描述',
    `tender_unit`           VARCHAR(100) DEFAULT NULL COMMENT '招标单位',
    `project_location`      VARCHAR(200) DEFAULT NULL COMMENT '项目地点',
    `contact_person`        VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    `contact_phone`         VARCHAR(30)  DEFAULT NULL COMMENT '联系电话',
    `expected_publish_time` DATETIME     DEFAULT NULL COMMENT '预期发布时间',
    `remark`                VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `review_type`           VARCHAR(30)  DEFAULT NULL COMMENT '评审类型: MANUAL/INTELLIGENT',
    `status`                VARCHAR(30)  NOT NULL DEFAULT 'DRAFT' COMMENT '项目状态: DRAFT/IN_PROGRESS/PENDING_DETECTION/DETECTING/DETECTION_PASSED/DETECTION_FAILED/DETECTION_SKIPPED/PUBLISHED/ARCHIVED/CANCELLED',
    `current_phase`         INT          DEFAULT NULL COMMENT '当前编制阶段: 1-基础信息 2-招标需求 3-评审项 4-文档集成 5-智能检测',
    `progress`              INT          DEFAULT NULL COMMENT '完成进度(百分比 0-100)',
    `template_id`           BIGINT       DEFAULT NULL COMMENT '使用的模板ID',
    `requirement_id`        BIGINT       DEFAULT NULL COMMENT '关联的业务需求ID',
    `requirement_source`    VARCHAR(30)  DEFAULT NULL COMMENT '需求来源: REFERENCE/SYSTEM_GENERATE',
    `requirement_content`   LONGTEXT     DEFAULT NULL COMMENT '招标需求内容',
    `generated_file_id`     BIGINT       DEFAULT NULL COMMENT '生成的招标文件ID(关联file_info)',
    `create_time`           DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`           DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`                   INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_code` (`project_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_category` (`project_category`),
    INDEX `idx_create_id` (`create_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI编制项目表';

-- =============================================
-- 2. AI编制项目版本表
-- 实体: com.jy.eleaitender.common.entity.core.AiProjectVersion
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_project_version` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `project_id`         BIGINT       NOT NULL COMMENT '项目ID',
    `version_no`         INT          NOT NULL COMMENT '版本号',
    `content_snapshot`   JSON         DEFAULT NULL COMMENT '内容快照JSON',
    `change_description` VARCHAR(500) DEFAULT NULL COMMENT '变更说明',
    `create_time`        DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`          BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`        DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`          BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`                INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_project_version` (`project_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI编制项目版本表';

-- =============================================
-- 3. 业务需求表
-- 实体: com.jy.eleaitender.common.entity.core.AiRequirement
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_requirement` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '需求ID',
    `requirement_name`       VARCHAR(100)  NOT NULL COMMENT '需求名称',
    `project_category`       VARCHAR(30)   NOT NULL COMMENT '项目类别',
    `project_type`           VARCHAR(30)   NOT NULL COMMENT '项目类型',
    `service_sub_type`       VARCHAR(50)   DEFAULT NULL COMMENT '服务子分类',
    `budget`                 DECIMAL(15,2) DEFAULT NULL COMMENT '预算价(元)',
    `requirement_description` VARCHAR(500) DEFAULT NULL COMMENT '需求描述',
    `match_mode`             VARCHAR(30)   DEFAULT NULL COMMENT '匹配模式: AUTO_MATCH/MANUAL_SELECT/UPLOAD',
    `matched_file_id`        BIGINT        DEFAULT NULL COMMENT '匹配的历史文件ID',
    `matched_similarity`     DECIMAL(5,2)  DEFAULT NULL COMMENT '匹配度百分比',
    `uploaded_file_id`       BIGINT        DEFAULT NULL COMMENT '上传的文件ID',
    `project_id`             BIGINT        DEFAULT NULL COMMENT '关联的项目ID',
    `content`                TEXT          DEFAULT NULL COMMENT '业务需求内容',
    `auto_save_content`      LONGTEXT      DEFAULT NULL COMMENT '自动保存内容(未提交的草稿)',
    `auto_save_time`         DATETIME      DEFAULT NULL COMMENT '自动保存时间',
    `status`                 VARCHAR(30)   NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/COMPLETED',
    `progress`               INT           DEFAULT NULL COMMENT '完成进度(百分比 0-100)',
    `create_time`            DATETIME      NOT NULL COMMENT '创建时间',
    `create_id`              BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`            VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`            DATETIME      NOT NULL COMMENT '修改时间',
    `modify_id`              BIGINT        NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`            VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`                    INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`              TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_id` (`create_id`),
    INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务需求表';

-- =============================================
-- 4. 招标文件模板表
-- 实体: com.jy.eleaitender.common.entity.core.AiTemplate
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_template` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code`         VARCHAR(50)  NOT NULL COMMENT '模板编码',
    `template_name`         VARCHAR(100) NOT NULL COMMENT '模板名称',
    `project_category`      VARCHAR(30)  NOT NULL COMMENT '适用项目类别',
    `project_type`          VARCHAR(30)  NOT NULL COMMENT '适用项目类型',
    `content`               LONGTEXT     NOT NULL COMMENT '模板内容(Markdown格式)',
    `structure_definition`  JSON         DEFAULT NULL COMMENT '模板结构定义JSON',
    `version_no`            INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `is_default`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认模板: 0-否 1-是',
    `status`                VARCHAR(30)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `create_time`           DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`           DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`                   INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template_code` (`template_code`),
    INDEX `idx_category_type` (`project_category`, `project_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招标文件模板表';

-- =============================================
-- 5. 评审项表（三级嵌套结构）
-- 实体: com.jy.eleaitender.common.entity.core.AiReviewItem
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_review_item` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '评审项ID',
    `project_id`   BIGINT        NOT NULL COMMENT '项目ID',
    `parent_id`    BIGINT        DEFAULT NULL COMMENT '父级ID, NULL表示顶级节点',
    `level`        INT           NOT NULL DEFAULT 1 COMMENT '层级: 1/2/3',
    `item_name`    VARCHAR(200)  NOT NULL COMMENT '评审项名称',
    `item_content` TEXT          DEFAULT NULL COMMENT '评审项内容',
    `sort_order`   INT           NOT NULL DEFAULT 0 COMMENT '排序号',
    `review_type`  VARCHAR(30)   DEFAULT NULL COMMENT '评审类型: COMPLIANCE/TECHNICAL/CREDIT/COMMERCIAL',
    `score`        DECIMAL(10,2) DEFAULT NULL COMMENT '分值(评审分值)',
    `max_score`    DECIMAL(10,2) DEFAULT NULL COMMENT '满分值(商务评审专用)',
    `weight`       DECIMAL(5,2)  DEFAULT NULL COMMENT '权重(百分比)',
    `subjectivity` VARCHAR(20)   DEFAULT NULL COMMENT '客观/主观: OBJECTIVE/SUBJECTIVE',
    `is_required`  TINYINT       DEFAULT NULL COMMENT '是否必审项: 0-否 1-是',
    `create_time`  DATETIME      NOT NULL COMMENT '创建时间',
    `create_id`    BIGINT        NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`  VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`  DATETIME      NOT NULL COMMENT '修改时间',
    `modify_id`    BIGINT        NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`  VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`          INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_project` (`project_id`),
    INDEX `idx_parent` (`parent_id`),
    INDEX `idx_review_type` (`review_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评审项表';

-- =============================================
-- 6. 用户政策文件表
-- 实体: com.jy.eleaitender.common.entity.core.AiPolicyFile
-- 说明: 用户上传的政策文件, 平台政策文件使用sup_policy_file表
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_policy_file` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_name`            VARCHAR(200) NOT NULL COMMENT '文件名称',
    `file_category`        VARCHAR(30)  DEFAULT NULL COMMENT '文件分类: LAW/REGULATION/POLICY',
    `applicable_category`  VARCHAR(30)  DEFAULT NULL COMMENT '适用项目类别',
    `file_id`              BIGINT       DEFAULT NULL COMMENT '关联file_info的ID',
    `file_size`            BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
    `file_type`            VARCHAR(20)  DEFAULT NULL COMMENT '文件格式: PDF/DOCX/DOC/XLSX',
    `description`          VARCHAR(500) DEFAULT NULL COMMENT '文件描述',
    `user_id`              BIGINT       DEFAULT NULL COMMENT '上传用户ID',
    `status`               TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    `create_time`          DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`            BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`          VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`          DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`            BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`          VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`                  INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category` (`applicable_category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户政策文件表';

-- =============================================
-- 7. 检测记录表
-- 实体: com.jy.eleaitender.common.entity.core.AiDetectionRecord (Core模块视图)
-- 实体: com.jy.eleaitender.common.entity.core.AiDetectionRecord (AI模块视图)
-- 说明: Core和AI模块共享同一张表, 各自定义了不同的实体字段
-- =============================================
CREATE TABLE IF NOT EXISTS `ai_detection_record` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '检测记录ID',
    `requirement_id`   BIGINT       DEFAULT NULL COMMENT '关联的业务需求ID(需求级检测时非空)',
    `project_id`       BIGINT       NOT NULL COMMENT '项目ID',
    `detection_type`   VARCHAR(50)  DEFAULT NULL COMMENT '检测类型: SENSITIVE_WORD/TYPO/POLICY_REVIEW/FORMAT_CHECK',
    `content_snapshot` TEXT         DEFAULT NULL COMMENT '检测内容快照',
    `result`           LONGTEXT     DEFAULT NULL COMMENT '检测结果(JSON)',
    `status`           VARCHAR(30)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED/AI_UNAVAILABLE/SKIPPED',
    `task_id`          BIGINT       DEFAULT NULL COMMENT '关联的AI任务ID',
    `policy_file_ids`  VARCHAR(500) DEFAULT NULL COMMENT '关联的政策文件ID列表(逗号分隔)',
    `started_at`       DATETIME     DEFAULT NULL COMMENT '开始时间',
    `completed_at`     DATETIME     DEFAULT NULL COMMENT '完成时间',
    `create_time`      DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`      DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`              INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_requirement` (`requirement_id`),
    INDEX `idx_project` (`project_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_detection_type` (`detection_type`),
    INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检测记录表';
