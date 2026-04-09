-- =========================================
-- EleTender 招标文件编制系统数据库初始化脚本
-- 数据库: ele_tender
-- 表前缀: td_
-- =========================================

CREATE DATABASE IF NOT EXISTS `ele_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_tender`;

DROP TABLE IF EXISTS `td_project_lock`;
CREATE TABLE `td_project_lock` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `owner_user_id` VARCHAR(64) NOT NULL COMMENT '首创人用户ID',
    `owner_user_name` VARCHAR(100) NOT NULL COMMENT '首创人用户名称',
    `owner_enterprise_id` VARCHAR(64) DEFAULT NULL COMMENT '首创人企业ID',
    `owner_enterprise_name` VARCHAR(200) DEFAULT NULL COMMENT '首创人企业名称',
    `owner_enterprise_code` VARCHAR(64) NOT NULL COMMENT '首创人统一信用代码',
    `owner_app_key` VARCHAR(64) NOT NULL COMMENT '首创来源业务系统appKey',
    `lock_time` DATETIME NOT NULL COMMENT '锁定时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目编制锁表';

DROP TABLE IF EXISTS `td_tender_document`;
CREATE TABLE `td_tender_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `biz_type` TINYINT NOT NULL COMMENT '业务类型',
    `biz_id` VARCHAR(64) NOT NULL COMMENT '业务ID',
    `compile_scope` VARCHAR(32) NOT NULL COMMENT '编制归属范围',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) DEFAULT NULL COMMENT '标段ID',
    `index_of` INT DEFAULT NULL COMMENT '标段序号',
    `project_code` VARCHAR(100) DEFAULT NULL COMMENT '项目编号',
    `project_name` VARCHAR(255) DEFAULT NULL COMMENT '项目名称',
    `purchase_method` VARCHAR(64) DEFAULT NULL COMMENT '采购方式',
    `eval_method` VARCHAR(64) DEFAULT NULL COMMENT '评标办法',
    `status` VARCHAR(32) NOT NULL COMMENT '编制单状态',
    `current_step_code` VARCHAR(32) NOT NULL COMMENT '当前步骤编码',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `source_version_no` INT DEFAULT NULL COMMENT '来源版本号',
    `latest_basic_info_sync_time` DATETIME DEFAULT NULL COMMENT '最近基本信息同步时间',
    `latest_bid_record_sync_time` DATETIME DEFAULT NULL COMMENT '最近标录同步时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_project_status` (`project_id`, `status`),
    INDEX `idx_biz_context` (`biz_type`, `biz_id`, `compile_scope`, `project_id`, `tender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件编制单主表';

DROP TABLE IF EXISTS `td_tender_document_step`;
-- 固定步骤顺序：
-- 1 BASIC_INFO
-- 2 PURCHASE_FILE
-- 3 BID_RECORD
-- 4 EVALUATION_RULE
-- 5 CHECK_ITEMS
-- 6 GENERATE_PACKAGE
CREATE TABLE `td_tender_document_step` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `step_code` VARCHAR(32) NOT NULL COMMENT '步骤编码',
    `step_order` INT NOT NULL COMMENT '步骤顺序',
    `step_status` VARCHAR(32) NOT NULL COMMENT '步骤状态',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `last_validate_time` DATETIME DEFAULT NULL COMMENT '最近校验时间',
    `last_validate_result` VARCHAR(1000) DEFAULT NULL COMMENT '最近校验结果',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_document_step` (`tender_document_id`, `step_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件步骤状态表';

DROP TABLE IF EXISTS `td_tender_document_snapshot`;
CREATE TABLE `td_tender_document_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `step_code` VARCHAR(32) NOT NULL COMMENT '步骤编码',
    `snapshot_json` LONGTEXT NOT NULL COMMENT '步骤快照JSON',
    `source_sync_time` DATETIME DEFAULT NULL COMMENT '源数据同步时间',
    `source_hash` VARCHAR(128) DEFAULT NULL COMMENT '源数据摘要',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_document_snapshot` (`tender_document_id`, `step_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件步骤快照表';

DROP TABLE IF EXISTS `td_tender_rule_header`;
CREATE TABLE `td_tender_rule_header` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) NOT NULL COMMENT '标段ID',
    `tender_name` VARCHAR(255) DEFAULT NULL COMMENT '标段名称',
    `eval_method` VARCHAR(64) NOT NULL COMMENT '评标办法',
    `node_category` VARCHAR(32) NOT NULL COMMENT '评审节点分类',
    `review_mode` VARCHAR(32) NOT NULL COMMENT '评审方式',
    `total_score` DECIMAL(12,2) DEFAULT NULL COMMENT '总分',
    `weight_rate` DECIMAL(12,2) DEFAULT NULL COMMENT '权重比例',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_rule_header` (`tender_document_id`, `tender_id`, `node_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件评审规则头表';

DROP TABLE IF EXISTS `td_tender_rule_score_config`;
CREATE TABLE `td_tender_rule_score_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `score_type` VARCHAR(32) DEFAULT NULL COMMENT '项目级统一分值类型',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_rule_score_config` (`tender_document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件项目级分值模式配置表';

DROP TABLE IF EXISTS `td_tender_rule_node`;
CREATE TABLE `td_tender_rule_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `rule_header_id` BIGINT NOT NULL COMMENT '规则头ID',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) NOT NULL COMMENT '标段ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父节点ID',
    `level` INT NOT NULL COMMENT '层级',
    `sort_no` INT NOT NULL COMMENT '同层排序',
    `item_content` LONGTEXT NOT NULL COMMENT '评审项内容',
    `score_min` DECIMAL(12,2) DEFAULT NULL COMMENT '分值下限',
    `score_max` DECIMAL(12,2) DEFAULT NULL COMMENT '分值上限',
    `score_standard` LONGTEXT DEFAULT NULL COMMENT '评分标准',
    `score_attribute` VARCHAR(32) DEFAULT NULL COMMENT '主客观分属性',
    `leaf_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '是否叶子节点',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_parent` (`rule_header_id`, `parent_id`),
    INDEX `idx_rule_level_sort` (`rule_header_id`, `level`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件评审规则节点表';

DROP TABLE IF EXISTS `td_tender_document_file`;
-- 文件角色：
-- PURCHASE_SOURCE_PDF 源采购文件
-- SIGNED_PDF 签章文件
-- FINAL_PACKAGE_FILE 生成数据包
-- COMPILE_INFO_PDF 采购文件编制信息
CREATE TABLE `td_tender_document_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) DEFAULT NULL COMMENT '标段ID',
    `scope_type` VARCHAR(32) NOT NULL COMMENT '归属范围',
    `file_role` VARCHAR(32) NOT NULL COMMENT '文件业务角色',
    `file_id` BIGINT NOT NULL COMMENT '文件服务ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
    `content_type` VARCHAR(128) DEFAULT NULL COMMENT '内容类型',
    `file_sha256` VARCHAR(64) DEFAULT NULL COMMENT '文件SHA-256',
    `active_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '是否当前有效',
    `created_time` DATETIME NOT NULL COMMENT '业务创建时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_document_file_role` (`tender_document_id`, `file_role`, `active_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件业务文件表';

DROP TABLE IF EXISTS `td_tender_document_version`;
CREATE TABLE `td_tender_document_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `version_no` INT NOT NULL COMMENT '版本号',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `status` VARCHAR(32) NOT NULL COMMENT '版本状态',
    `generate_time` DATETIME NOT NULL COMMENT '生成时间',
    `summary_json` LONGTEXT DEFAULT NULL COMMENT '版本摘要',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_document_version` (`tender_document_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件完成版本表';

DROP TABLE IF EXISTS `td_tender_document_callback`;
DROP TABLE IF EXISTS `td_tender_document_callback_counter`;
DROP TABLE IF EXISTS `td_tender_ca_keys_snapshot`;
CREATE TABLE `td_tender_ca_keys_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `version_no` INT NOT NULL COMMENT '生成版本号',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) DEFAULT NULL COMMENT '标段ID',
    `source_app_key` VARCHAR(64) DEFAULT NULL COMMENT '来源业务系统appKey',
    `trace_id` VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    `captured_time` DATETIME NOT NULL COMMENT '快照采集时间',
    `ca_keys_count` INT NOT NULL DEFAULT 0 COMMENT 'caKeysInfo条数',
    `ca_keys_json` LONGTEXT NOT NULL COMMENT '开标时有效caKeysInfo快照JSON',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_document_version` (`tender_document_id`, `version_no`),
    INDEX `idx_project_tender` (`project_id`, `tender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件生成时caKeysInfo快照表';

CREATE TABLE `td_tender_document_callback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `version_no` INT NOT NULL COMMENT '版本号',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) DEFAULT NULL COMMENT '标段ID',
    `file_role` VARCHAR(32) NOT NULL COMMENT '回传文件角色',
    `file_id` BIGINT NOT NULL COMMENT '文件服务ID',
    `callback_status` VARCHAR(32) NOT NULL COMMENT '回传状态',
    `callback_time` DATETIME NOT NULL COMMENT '回传时间',
    `response_code` VARCHAR(64) DEFAULT NULL COMMENT '响应码',
    `response_message` VARCHAR(2000) DEFAULT NULL COMMENT '响应信息',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人名称',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_document_callback` (`tender_document_id`, `version_no`, `file_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件回传记录表';

CREATE TABLE `td_tender_document_callback_counter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `version_no` INT NOT NULL COMMENT '版本号',
    `file_role` VARCHAR(32) NOT NULL COMMENT '回传文件角色',
    `tender_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '标段ID，项目级为空字符串',
    `current_retry_count` INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_callback_counter` (`tender_document_id`, `version_no`, `file_role`, `tender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件回传重试计数器表';

DROP TABLE IF EXISTS `td_tender_document_generation_record`;
CREATE TABLE `td_tender_document_generation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tender_document_id` BIGINT NOT NULL COMMENT '编制单ID',
    `version_no` INT NOT NULL COMMENT '生成时版本号',
    `project_id` VARCHAR(64) NOT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) DEFAULT NULL COMMENT '标段ID',
    `generate_status` VARCHAR(32) NOT NULL COMMENT '生成状态',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    `error_code` VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    `error_message` VARCHAR(2000) DEFAULT NULL COMMENT '错误信息',
    `trace_id` VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人名称',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_generation_record_document` (`tender_document_id`, `id`),
    INDEX `idx_generation_record_status` (`tender_document_id`, `generate_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件生成记录表';
