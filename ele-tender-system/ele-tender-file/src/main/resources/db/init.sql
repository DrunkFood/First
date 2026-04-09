-- =========================================
-- EleTender 文件服务数据库初始化脚本
-- 数据库: ele_tender
-- 表前缀: file_
-- =========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ele_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_tender`;

-- =========================================
-- 文件信息表
-- =========================================
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，即fileId',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '存储路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `file_sha256` VARCHAR(64) NOT NULL COMMENT '文件SHA-256',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型(main-version/plugin)',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_file_sha256` (`file_sha256`),
    INDEX `idx_biz_type` (`biz_type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- =========================================
-- 系统访问日志表
-- =========================================
DROP TABLE IF EXISTS `sup_access_log`;
CREATE TABLE `sup_access_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    `service_name` VARCHAR(64) NOT NULL COMMENT '服务名称',
    `log_type` VARCHAR(32) NOT NULL COMMENT '日志类型',
    `http_method` VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法',
    `request_uri` VARCHAR(500) DEFAULT NULL COMMENT '请求URI',
    `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    `status_code` INT DEFAULT NULL COMMENT '响应状态码',
    `success_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功: 0-失败, 1-成功',
    `elapsed_ms` BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    `token_type` VARCHAR(32) DEFAULT NULL COMMENT 'Token类型',
    `app_key` VARCHAR(64) DEFAULT NULL COMMENT '应用Key',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    `user_name` VARCHAR(100) DEFAULT NULL COMMENT '用户名称',
    `enterprise_id` VARCHAR(64) DEFAULT NULL COMMENT '企业ID',
    `enterprise_name` VARCHAR(200) DEFAULT NULL COMMENT '企业名称',
    `biz_type` VARCHAR(32) DEFAULT NULL COMMENT '业务类型',
    `biz_id` VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
    `project_id` VARCHAR(64) DEFAULT NULL COMMENT '项目ID',
    `tender_id` VARCHAR(64) DEFAULT NULL COMMENT '标段ID',
    `file_id` VARCHAR(64) DEFAULT NULL COMMENT '文件ID',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名称',
    `request_headers` TEXT DEFAULT NULL COMMENT '请求头摘要',
    `request_body` TEXT DEFAULT NULL COMMENT '请求体摘要',
    `response_body` TEXT DEFAULT NULL COMMENT '响应体摘要',
    `error_type` VARCHAR(128) DEFAULT NULL COMMENT '异常类型',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '异常信息',
    `create_time` DATETIME NOT NULL COMMENT '记录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_trace_id` (`trace_id`),
    INDEX `idx_service_name` (`service_name`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_biz_context` (`biz_type`, `biz_id`, `project_id`, `tender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统访问日志表';
