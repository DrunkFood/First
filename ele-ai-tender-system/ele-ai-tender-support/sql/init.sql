-- =========================================
-- Ele AI Tender 支撑中心数据库初始化脚本
-- 数据库: ele_ai_tender
-- 表前缀: sup_
-- Redis: db=6
-- =========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ele_ai_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_ai_tender`;

-- =========================================
-- 用户表
-- =========================================
DROP TABLE IF EXISTS `sup_user`;
CREATE TABLE `sup_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_username` (`username`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- =========================================
-- 角色表
-- =========================================
DROP TABLE IF EXISTS `sup_role`;
CREATE TABLE `sup_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- =========================================
-- 用户角色关联表
-- =========================================
DROP TABLE IF EXISTS `sup_user_role`;
CREATE TABLE `sup_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_user_role` (`user_id`, `role_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =========================================
-- 菜单/权限表
-- =========================================
DROP TABLE IF EXISTS `sup_menu`;
CREATE TABLE `sup_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID, 0表示根菜单',
    `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `menu_code` VARCHAR(100) DEFAULT NULL COMMENT '菜单编码',
    `menu_type` TINYINT NOT NULL COMMENT '类型: 1-菜单, 2-按钮',
    `menu_url` VARCHAR(255) DEFAULT NULL COMMENT '菜单URL',
    `permission` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_menu_code` (`menu_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- =========================================
-- 角色菜单关联表
-- =========================================
DROP TABLE IF EXISTS `sup_role_menu`;
CREATE TABLE `sup_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_role_menu` (`role_id`, `menu_id`),
    INDEX `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- =========================================
-- 接入系统表
-- =========================================
DROP TABLE IF EXISTS `sup_access_system`;
CREATE TABLE `sup_access_system` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '系统ID',
    `system_name` VARCHAR(100) NOT NULL COMMENT '系统名称',
    `system_url` VARCHAR(255) DEFAULT NULL COMMENT '系统URL',
    `app_key` VARCHAR(64) NOT NULL COMMENT '应用Key',
    `app_secret` VARCHAR(128) NOT NULL COMMENT '应用密钥',
    `expire_time` DATETIME DEFAULT NULL COMMENT '有效期截止时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '系统描述',
    `tender_document_suffix` VARCHAR(32) DEFAULT NULL COMMENT '招标文件后缀(如.HzctZbs)',
    `bid_document_suffix` VARCHAR(32) DEFAULT NULL COMMENT '投标文件后缀(如.HzctTbs)',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_app_key` (`app_key`),
    INDEX `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接入系统表';

-- =========================================
-- 主系统版本表
-- =========================================
DROP TABLE IF EXISTS `sup_main_version`;
CREATE TABLE `sup_main_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `system_name` VARCHAR(100) NOT NULL COMMENT '主系统名称',
    `version_number` VARCHAR(50) NOT NULL COMMENT '版本号',
    `file_id` BIGINT NOT NULL COMMENT '文件ID',
    `release_notes` TEXT DEFAULT NULL COMMENT '发布说明',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已下线',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_system_version` (`system_name`, `version_number`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主系统版本表';

-- =========================================
-- 插件版本表
-- =========================================
DROP TABLE IF EXISTS `sup_plugin_version`;
CREATE TABLE `sup_plugin_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '插件版本ID',
    `plugin_name` VARCHAR(100) NOT NULL COMMENT '插件名称',
    `version_number` VARCHAR(50) NOT NULL COMMENT '版本号',
    `compatible_main_version` VARCHAR(50) NOT NULL COMMENT '适配的主系统版本',
    `file_id` BIGINT NOT NULL COMMENT '文件ID',
    `release_notes` TEXT DEFAULT NULL COMMENT '发布说明',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已下线',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `create_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time` DATETIME NOT NULL COMMENT '修改时间',
    `modify_id` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_plugin_version` (`plugin_name`, `version_number`),
    INDEX `idx_compatible_version` (`compatible_main_version`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件版本表';

-- =========================================
-- 操作日志表
-- =========================================
DROP TABLE IF EXISTS `sup_operation_log`;
CREATE TABLE `sup_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `user_name` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作类型',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `execute_time` BIGINT DEFAULT NULL COMMENT '执行时长(毫秒)',
    `create_time` DATETIME NOT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

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

-- =========================================
-- 初始化数据
-- =========================================

-- 初始化管理员用户 (密码: 123456)
-- 密码加密格式: BCrypt
INSERT INTO `sup_user` (`username`, `password`, `real_name`, `status`, `create_time`, `modify_time`) 
VALUES ('admin', '$2a$10$305PBEMiyk/uFtrUC.ywKO731rAgH0dxt3ynt9bTCQs.lqYJkXKZC', '系统管理员', 1, NOW(), NOW());

-- 初始化管理员角色
INSERT INTO `sup_role` (`role_code`, `role_name`, `description`, `status`, `create_time`, `modify_time`) 
VALUES ('ADMIN', '管理员', '系统管理员角色，拥有所有权限', 1, NOW(), NOW());

-- 关联管理员用户和角色
INSERT INTO `sup_user_role` (`user_id`, `role_id`, `create_time`, `modify_time`) 
VALUES (1, 1, NOW(), NOW());

-- 初始化菜单数据（AI编制系统专属菜单）
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
-- 一级菜单
(0, '系统管理', 'system', 1, '/system', NULL, 1, 1, NOW(), NOW()),
(0, '版本管理', 'version', 1, '/version', NULL, 2, 1, NOW(), NOW()),
(0, 'AI编制', 'ai-tender', 1, '/ai-tender', NULL, 3, 1, NOW(), NOW()),

-- 用户管理
(1, '用户管理', 'user', 1, '/system/user', 'user:view', 1, 1, NOW(), NOW()),
(4, '创建用户', 'user:create', 2, NULL, 'user:create', 1, 1, NOW(), NOW()),
(4, '编辑用户', 'user:update', 2, NULL, 'user:update', 2, 1, NOW(), NOW()),
(4, '删除用户', 'user:delete', 2, NULL, 'user:delete', 3, 1, NOW(), NOW()),
(4, '启用/禁用', 'user:status', 2, NULL, 'user:status', 4, 1, NOW(), NOW()),
(4, '重置密码', 'user:reset-password', 2, NULL, 'user:reset-password', 5, 1, NOW(), NOW()),

-- 角色管理
(1, '角色管理', 'role', 1, '/system/role', 'role:view', 2, 1, NOW(), NOW()),
(10, '创建角色', 'role:create', 2, NULL, 'role:create', 1, 1, NOW(), NOW()),
(10, '编辑角色', 'role:update', 2, NULL, 'role:update', 2, 1, NOW(), NOW()),
(10, '删除角色', 'role:delete', 2, NULL, 'role:delete', 3, 1, NOW(), NOW()),
(10, '分配权限', 'role:assign-permission', 2, NULL, 'role:assign-permission', 4, 1, NOW(), NOW()),

-- 菜单管理
(1, '菜单管理', 'menu', 1, '/system/menu', 'menu:view', 3, 1, NOW(), NOW()),
(15, '创建菜单', 'menu:create', 2, NULL, 'menu:create', 1, 1, NOW(), NOW()),
(15, '编辑菜单', 'menu:update', 2, NULL, 'menu:update', 2, 1, NOW(), NOW()),
(15, '删除菜单', 'menu:delete', 2, NULL, 'menu:delete', 3, 1, NOW(), NOW()),

-- 接入系统管理
(1, '接入系统', 'access-system', 1, '/system/access-system', 'system:view', 4, 1, NOW(), NOW()),
(19, '创建系统', 'system:create', 2, NULL, 'system:create', 1, 1, NOW(), NOW()),
(19, '编辑系统', 'system:update', 2, NULL, 'system:update', 2, 1, NOW(), NOW()),
(19, '删除系统', 'system:delete', 2, NULL, 'system:delete', 3, 1, NOW(), NOW()),
(19, '启用/禁用', 'system:status', 2, NULL, 'system:status', 4, 1, NOW(), NOW()),
(19, '重新生成密钥', 'system:regenerate-secret', 2, NULL, 'system:regenerate-secret', 5, 1, NOW(), NOW()),

-- 访问日志
(1, '访问日志', 'access-log', 1, '/system/access-log', 'access-log:view', 5, 1, NOW(), NOW()),

-- 主系统版本管理
(2, '主系统版本', 'main-version', 1, '/version/main', 'main-version:view', 1, 1, NOW(), NOW()),
(26, '创建版本', 'main-version:create', 2, NULL, 'main-version:create', 1, 1, NOW(), NOW()),
(26, '编辑版本', 'main-version:update', 2, NULL, 'main-version:update', 2, 1, NOW(), NOW()),
(26, '删除版本', 'main-version:delete', 2, NULL, 'main-version:delete', 3, 1, NOW(), NOW()),
(26, '发布版本', 'main-version:publish', 2, NULL, 'main-version:publish', 4, 1, NOW(), NOW()),
(26, '下线版本', 'main-version:offline', 2, NULL, 'main-version:offline', 5, 1, NOW(), NOW()),

-- 插件版本管理
(2, '插件版本', 'plugin-version', 1, '/version/plugin', 'plugin-version:view', 2, 1, NOW(), NOW()),
(32, '创建插件', 'plugin-version:create', 2, NULL, 'plugin-version:create', 1, 1, NOW(), NOW()),
(32, '编辑插件', 'plugin-version:update', 2, NULL, 'plugin-version:update', 2, 1, NOW(), NOW()),
(32, '删除插件', 'plugin-version:delete', 2, NULL, 'plugin-version:delete', 3, 1, NOW(), NOW()),
(32, '发布插件', 'plugin-version:publish', 2, NULL, 'plugin-version:publish', 4, 1, NOW(), NOW()),
(32, '下线插件', 'plugin-version:offline', 2, NULL, 'plugin-version:offline', 5, 1, NOW(), NOW()),

-- AI编制菜单（新增）
(3, '项目管理', 'ai-project', 1, '/ai-tender/project', 'ai-project:view', 1, 1, NOW(), NOW()),
(38, '创建项目', 'ai-project:create', 2, NULL, 'ai-project:create', 1, 1, NOW(), NOW()),
(38, '编辑项目', 'ai-project:update', 2, NULL, 'ai-project:update', 2, 1, NOW(), NOW()),
(38, '删除项目', 'ai-project:delete', 2, NULL, 'ai-project:delete', 3, 1, NOW(), NOW()),

(3, '招标文件编制', 'ai-document', 1, '/ai-tender/document', 'ai-document:view', 2, 1, NOW(), NOW()),
(42, '创建招标文件', 'ai-document:create', 2, NULL, 'ai-document:create', 1, 1, NOW(), NOW()),
(42, 'AI生成', 'ai-document:generate', 2, NULL, 'ai-document:generate', 2, 1, NOW(), NOW()),
(42, '编辑招标文件', 'ai-document:update', 2, NULL, 'ai-document:update', 3, 1, NOW(), NOW()),
(42, '导出招标文件', 'ai-document:export', 2, NULL, 'ai-document:export', 4, 1, NOW(), NOW());

-- 给管理员角色分配所有菜单权限
INSERT INTO `sup_role_menu` (`role_id`, `menu_id`, `create_time`, `modify_time`)
SELECT 1, id, NOW(), NOW() FROM `sup_menu`;

