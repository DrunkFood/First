-- =====================================================
-- Ele AI Tender 支撑中心数据库初始化脚本
-- 数据库: ele_ai_tender
-- 表前缀: sup_
-- Redis:  db=6
-- 合并自: init.sql + phase1-support-enhance.sql
--        + init-bid-user-role.sql + init-sms-code.sql
--        + phase2-core-business-init.sql (sup_message)
-- 执行顺序: 建库 → 建表 → 基础数据 → 菜单+权限
-- =====================================================

CREATE DATABASE IF NOT EXISTS `ele_ai_tender` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ele_ai_tender`;

-- =====================================================
-- 第一部分: 建表
-- =====================================================

-- -----------------------------------------------
-- 1. 系统用户表
-- -----------------------------------------------
DROP TABLE IF EXISTS `sup_user`;
CREATE TABLE `sup_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
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
    UNIQUE INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- -----------------------------------------------
-- 2. 系统角色表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 3. 用户角色关联表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 4. 菜单/权限表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 5. 角色菜单关联表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 6. 接入系统表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 7. 主系统版本表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 8. 插件版本表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 9. 操作日志表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 10. 系统访问日志表
-- -----------------------------------------------
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

-- -----------------------------------------------
-- 11. 系统参数表
-- -----------------------------------------------
DROP TABLE IF EXISTS `sup_sys_parameter`;
CREATE TABLE `sup_sys_parameter` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `param_group`   VARCHAR(50)  NOT NULL COMMENT '参数分组: SYSTEM/SWITCH/AI_RULE',
    `param_key`     VARCHAR(100) NOT NULL COMMENT '参数键',
    `param_value`   VARCHAR(2000) DEFAULT NULL COMMENT '参数值',
    `param_type`    VARCHAR(20)  NOT NULL DEFAULT 'STRING' COMMENT '值类型: STRING/NUMBER/BOOLEAN/JSON',
    `param_name`    VARCHAR(200) NOT NULL COMMENT '参数中文名',
    `description`   VARCHAR(500) DEFAULT NULL COMMENT '参数说明',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '组内排序',
    `create_time`   DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`   DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`           INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_param_key` (`param_key`),
    INDEX `idx_param_group` (`param_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- -----------------------------------------------
-- 12. 系统政策文件表
-- -----------------------------------------------
DROP TABLE IF EXISTS `sup_policy_file`;
CREATE TABLE `sup_policy_file` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name`             VARCHAR(200) NOT NULL COMMENT '文件名称',
    `file_category`         VARCHAR(30)  NOT NULL COMMENT '文件分类: LAW(法律法规)/REGULATION(规章制度)/POLICY(政策文件)',
    `applicable_category`   VARCHAR(100) DEFAULT NULL COMMENT '适用项目类别，多个用逗号分隔: SMALL_TRADE/GOVERNMENT_PROCUREMENT/COMPREHENSIVE_TRADE',
    `file_id`               BIGINT       DEFAULT NULL COMMENT '关联文件服务的file_id',
    `file_size`             BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
    `file_type`             VARCHAR(20)  DEFAULT NULL COMMENT '文件格式: PDF/DOCX/DOC/XLSX',
    `description`           VARCHAR(500) DEFAULT NULL COMMENT '文件描述',
    `status`                TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time`           DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`           DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`                   INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_file_category` (`file_category`),
    INDEX `idx_applicable_category` (`applicable_category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统政策文件表';

-- =====================================================
-- 13. AI模型配置表 (sup_model_config)
--    支撑中心管理，AI模块读取（只读）
-- =====================================================
CREATE TABLE IF NOT EXISTS `sup_model_config` (
                                                  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
                                                  `model_name`      VARCHAR(100) NOT NULL COMMENT '模型名称',
                                                  `model_type`      VARCHAR(20)  NOT NULL COMMENT '模型类型: LOCAL(本地微调)/CLOUD(云端大模型)/PRIVATE(私有化部署)',
                                                  `provider`        VARCHAR(20)  NOT NULL DEFAULT 'OPENAI' COMMENT '模型供应商: OPENAI(兼容协议)/ZHIPU(智谱AI)',
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
INSERT INTO `sup_model_config` (`model_name`, `model_type`, `provider`, `api_endpoint`, `api_key`, `model_params`, `usage_scenario`, `is_active`, `create_time`, `modify_time`)
VALUES ('DeepSeek-Chat', 'CLOUD', 'OPENAI', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.7, "maxTokens": 4096, "topP": 0.9, "model": "deepseek-chat"}', 'GENERATION', 1, NOW(), NOW()),
       ('DeepSeek-Chat', 'CLOUD', 'OPENAI', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.3, "maxTokens": 2048, "topP": 0.85, "model": "deepseek-chat"}', 'OPTIMIZATION', 1, NOW(), NOW()),
       ('DeepSeek-Chat', 'CLOUD', 'OPENAI', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.1, "maxTokens": 4096, "topP": 0.8, "model": "deepseek-chat"}', 'DETECTION', 1, NOW(), NOW()),
       ('DeepSeek-V4-Pro', 'CLOUD', 'OPENAI', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.7, "maxTokens": 4096, "topP": 0.9, "model": "deepseek-v4-pro"}', 'GENERATION', 1, NOW(), NOW()),
       ('DeepSeek-V4-Pro', 'CLOUD', 'OPENAI', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.3, "maxTokens": 2048, "topP": 0.85, "model": "deepseek-v4-pro"}', 'OPTIMIZATION', 1, NOW(), NOW()),
       ('DeepSeek-V4-Pro', 'CLOUD', 'OPENAI', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', '{"temperature": 0.1, "maxTokens": 4096, "topP": 0.8, "model": "deepseek-v4-pro"}', 'DETECTION', 1, NOW(), NOW());

-- -----------------------------------------------
-- 13. 模型路由规则表
-- -----------------------------------------------
DROP TABLE IF EXISTS `sup_model_route_rule`;
CREATE TABLE `sup_model_route_rule` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `usage_scenario`    VARCHAR(50)  NOT NULL COMMENT '使用场景: GENERATION/OPTIMIZATION/DETECTION',
    `primary_model_id`  BIGINT       NOT NULL COMMENT '优先模型ID(关联sup_model_config.id)',
    `fallback_model_id` BIGINT       DEFAULT NULL COMMENT '降级模型ID(关联sup_model_config.id)',
    `priority`          INT          NOT NULL DEFAULT 0 COMMENT '优先级(值越小优先级越高)',
    `is_active`         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用: 0-停用, 1-启用',
    `description`       VARCHAR(500) DEFAULT NULL COMMENT '规则描述',
    `create_time`       DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`       DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`               INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_usage_scenario` (`usage_scenario`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型路由规则表';

-- -----------------------------------------------
-- 14. 消息表
-- -----------------------------------------------
DROP TABLE IF EXISTS `sup_message`;
CREATE TABLE `sup_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content` TEXT COMMENT '消息内容',
    `message_type` VARCHAR(20) NOT NULL COMMENT '消息类型:SYSTEM(系统)/AUDIT(审核)/DETECTION(检测)/WARNING(警告)',
    `biz_id` BIGINT COMMENT '关联业务ID(如项目ID、需求ID等)',
    `biz_type` VARCHAR(20) COMMENT '关联业务类型:PROJECT/REQUIREMENT/DETECTION/TEMPLATE',
    `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读:0未读/1已读',
    `read_time` DATETIME COMMENT '阅读时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_id` BIGINT COMMENT '创建人ID',
    `create_name` VARCHAR(50) COMMENT '创建人名称',
    `modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_id` BIGINT COMMENT '修改人ID',
    `modify_name` VARCHAR(50) COMMENT '修改人名称',
    `ver` INT DEFAULT 0 COMMENT '乐观锁版本号',
    `is_delete` INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX `idx_user` (`user_id`),
    INDEX `idx_type` (`message_type`),
    INDEX `idx_read` (`is_read`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- -----------------------------------------------
-- 15. 短信验证码表
-- -----------------------------------------------
DROP TABLE IF EXISTS `sup_sms_code`;
CREATE TABLE `sup_sms_code` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码(6位数字)',
    `scene` VARCHAR(50) NOT NULL COMMENT '使用场景:LOGIN(登录)/REGISTER(注册)/RESET_PWD(重置密码)/BIND_PHONE(绑定手机)',
    `status` VARCHAR(20) DEFAULT 'UNUSED' COMMENT '状态:UNUSED(未使用)/USED(已使用)/EXPIRED(已过期)',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间(一般5-10分钟)',
    `ip_address` VARCHAR(50) COMMENT '发送IP地址(防刷)',
    `used_time` DATETIME COMMENT '使用时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_phone_status` (`phone`, `status`),
    INDEX `idx_expire_time` (`expire_time`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码表';

-- =============================================
-- 4. 招标文件模板表
-- 实体: com.jy.eleaitender.common.entity.core.SupTemplate
-- =============================================
CREATE TABLE IF NOT EXISTS `sup_template` (
                                              `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
                                              `template_name`         VARCHAR(100) NOT NULL COMMENT '模板名称',
                                              `project_category`      VARCHAR(30)  NOT NULL COMMENT '适用项目类别',
                                              `project_type`          VARCHAR(30)  NOT NULL COMMENT '适用项目类型',
                                              `file_id`               BIGINT       DEFAULT NULL COMMENT '模板文件ID(关联file_info)',
                                              `content`               LONGTEXT     DEFAULT NULL COMMENT '模板用途说明',
                                              `structure_definition`  JSON         DEFAULT NULL COMMENT '模板结构定义JSON',
                                              `review_config`         JSON         DEFAULT NULL COMMENT '评审项配置JSON',
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
                                              INDEX `idx_category_type` (`project_category`, `project_type`),
                                              INDEX `idx_status` (`status`),
                                              INDEX `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招标文件模板表';

-- =====================================================
-- 第二部分: 基础数据
-- =====================================================

-- -----------------------------------------------
-- 1. 管理员用户 (密码: 123456, BCrypt加密)
-- -----------------------------------------------
INSERT INTO `sup_user` (`username`, `password`, `real_name`, `phone`, `status`, `create_time`, `modify_time`)
VALUES ('admin', '$2a$10$305PBEMiyk/uFtrUC.ywKO731rAgH0dxt3ynt9bTCQs.lqYJkXKZC', '系统管理员', '13900139000', 1, NOW(), NOW());

-- -----------------------------------------------
-- 2. 管理员角色 + 业务用户角色
-- -----------------------------------------------
INSERT INTO `sup_role` (`role_code`, `role_name`, `description`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
VALUES ('ADMIN', '管理员', '系统管理员角色，拥有所有权限', 1, NOW(), 0, '', NOW(), 0, '');

INSERT INTO `sup_role` (`role_code`, `role_name`, `description`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
VALUES ('BID_USER', '业务用户', 'AI招标文件编制业务用户，仅可访问AI编制系统', 1, NOW(), 1, 'admin', NOW(), 1, 'admin');

-- -----------------------------------------------
-- 3. 关联管理员用户和角色
-- -----------------------------------------------
INSERT INTO `sup_user_role` (`user_id`, `role_id`, `create_time`, `modify_time`)
VALUES (1, 1, NOW(), NOW());

-- -----------------------------------------------
-- 4. 测试业务用户 (密码: 123456, BCrypt加密)
-- -----------------------------------------------
INSERT INTO `sup_user` (`username`, `password`, `real_name`, `phone`, `email`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
VALUES ('testuser', '$2a$10$305PBEMiyk/uFtrUC.ywKO731rAgH0dxt3ynt9bTCQs.lqYJkXKZC', '测试用户', '13800138000', 'testuser@example.com', 1, NOW(), 1, 'admin', NOW(), 1, 'admin');

INSERT INTO `sup_user_role` (`user_id`, `role_id`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
SELECT u.id, r.id, NOW(), 1, 'admin', NOW(), 1, 'admin'
FROM `sup_user` u, `sup_role` r
WHERE u.username = 'testuser' AND r.role_code = 'BID_USER';

-- -----------------------------------------------
-- 5. 系统参数初始数据
-- -----------------------------------------------
INSERT INTO `sup_sys_parameter` (`param_group`, `param_key`, `param_value`, `param_type`, `param_name`, `description`, `sort_order`, `create_time`, `modify_time`) VALUES
('SYSTEM', 'system_name', '招标文件AI编制工具', 'STRING', '系统名称', '系统显示名称', 1, NOW(), NOW()),
('SYSTEM', 'max_file_size_mb', '50', 'NUMBER', '最大文件大小(MB)', '上传文件大小限制，单位MB', 2, NOW(), NOW()),
('SYSTEM', 'session_timeout_min', '120', 'NUMBER', '会话超时时间(分钟)', '用户会话无操作超时时间', 3, NOW(), NOW()),
('SYSTEM', 'detection_fail_limit', '3', 'NUMBER', '检测不通过上限次数', '项目检测不通过的最大允许次数', 4, NOW(), NOW()),
('SWITCH', 'enable_ai_chat', 'true', 'BOOLEAN', '启用AI对话功能', '开启后用户可使用浮动AI对话组件', 1, NOW(), NOW()),
('SWITCH', 'enable_detection', 'true', 'BOOLEAN', '启用智能检测功能', '开启后系统将自动进行文档检测', 2, NOW(), NOW()),
('SWITCH', 'enable_notification', 'true', 'BOOLEAN', '启用消息通知', '开启后用户将收到系统消息通知', 3, NOW(), NOW()),
('AI_RULE', 'fairness_rules', '检查是否指定特定品牌或供应商，是否设置不合理门槛，是否存在地域限制等内容。', 'JSON', '公平竞争检测规则', 'AI公平竞争检测使用的规则文本', 1, NOW(), NOW()),
('AI_RULE', 'compliance_rules', '检查是否符合相关法律法规要求，是否符合招标文件格式规范，是否存在遗漏必要条款等内容。', 'JSON', '合规性检查规则', 'AI合规性检查使用的规则文本', 2, NOW(), NOW()),
('AI_THREAD_POOL', 'ai_task_timeout_minutes', '10', 'NUMBER', '单任务超时时间(分钟)', '单个AI任务执行超时时间', 5, NOW(), NOW()),
('AI_THREAD_POOL', 'user_max_pending_tasks', '5', 'NUMBER', '每用户最多发起任务数', '每个用户可发起的未完成AI任务上限，超过直接拒绝', 6, NOW(), NOW()),
('AI_THREAD_POOL', 'user_max_concurrent_tasks', '2', 'NUMBER', '每用户同时执行任务数', '每个用户可同时执行的AI任务上限，超过排队等待', 7, NOW(), NOW()),
('AI_THREAD_POOL', 'global_max_pending_tasks', '100', 'NUMBER', '全局最多发起任务数', '系统全局未完成AI任务上限，超过直接拒绝', 8, NOW(), NOW()),
('AI_THREAD_POOL', 'global_max_concurrent_tasks', '10', 'NUMBER', '全局同时执行任务数', '系统全局可同时执行的AI任务上限，超过排队等待', 9, NOW(), NOW());


-- =====================================================
-- 第三部分: 菜单数据
-- =====================================================

INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `icon`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
-- ========== 一级菜单 ==========
(0, '系统管理', 'system', 1, '/system', NULL, NULL, 1, 1, NOW(), NOW()),
(0, '版本管理', 'version', 1, '/version', NULL, NULL, 2, 1, NOW(), NOW()),
(0, '政策文件审查库', 'policy-file', 1, '/policy-file', 'policy-file:view', NULL, 3, 1, NOW(), NOW()),
(0, '消息中心', 'message', 1, '/message', NULL, NULL, 4, 1, NOW(), NOW()),
(0, '统计分析', 'statistics', 1, '/statistics', NULL, NULL, 5, 1, NOW(), NOW()),
(0, '模型路由', 'model-route', 1, '/model-route', 'model-route:view', NULL, 6, 1, NOW(), NOW()),
(0, 'AI招标文件编制', 'ai-tender-biz', 1, '/ai-tender', NULL, NULL, 10, 1, NOW(), NOW()),

-- ========== 系统管理子菜单 ==========
-- 用户管理 (parent=1)
(1, '用户管理', 'user', 1, '/system/user', 'user:view', NULL, 1, 1, NOW(), NOW()),
(8, '创建用户', 'user:create', 2, NULL, 'user:create', NULL, 1, 1, NOW(), NOW()),
(8, '编辑用户', 'user:update', 2, NULL, 'user:update', NULL, 2, 1, NOW(), NOW()),
(8, '删除用户', 'user:delete', 2, NULL, 'user:delete', NULL, 3, 1, NOW(), NOW()),
(8, '启用/禁用', 'user:status', 2, NULL, 'user:status', NULL, 4, 1, NOW(), NOW()),
(8, '重置密码', 'user:reset-password', 2, NULL, 'user:reset-password', NULL, 5, 1, NOW(), NOW()),
-- 角色管理 (parent=1)
(1, '角色管理', 'role', 1, '/system/role', 'role:view', NULL, 2, 1, NOW(), NOW()),
(14, '创建角色', 'role:create', 2, NULL, 'role:create', NULL, 1, 1, NOW(), NOW()),
(14, '编辑角色', 'role:update', 2, NULL, 'role:update', NULL, 2, 1, NOW(), NOW()),
(14, '删除角色', 'role:delete', 2, NULL, 'role:delete', NULL, 3, 1, NOW(), NOW()),
(14, '分配权限', 'role:assign-permission', 2, NULL, 'role:assign-permission', NULL, 4, 1, NOW(), NOW()),
-- 菜单管理 (parent=1)
(1, '菜单管理', 'menu', 1, '/system/menu', 'menu:view', NULL, 3, 1, NOW(), NOW()),
(19, '创建菜单', 'menu:create', 2, NULL, 'menu:create', NULL, 1, 1, NOW(), NOW()),
(19, '编辑菜单', 'menu:update', 2, NULL, 'menu:update', NULL, 2, 1, NOW(), NOW()),
(19, '删除菜单', 'menu:delete', 2, NULL, 'menu:delete', NULL, 3, 1, NOW(), NOW()),
-- 接入系统 (parent=1)
(1, '接入系统', 'access-system', 1, '/system/access-system', 'system:view', NULL, 4, 1, NOW(), NOW()),
(23, '创建系统', 'system:create', 2, NULL, 'system:create', NULL, 1, 1, NOW(), NOW()),
(23, '编辑系统', 'system:update', 2, NULL, 'system:update', NULL, 2, 1, NOW(), NOW()),
(23, '删除系统', 'system:delete', 2, NULL, 'system:delete', NULL, 3, 1, NOW(), NOW()),
(23, '启用/禁用', 'system:status', 2, NULL, 'system:status', NULL, 4, 1, NOW(), NOW()),
(23, '重新生成密钥', 'system:regenerate-secret', 2, NULL, 'system:regenerate-secret', NULL, 5, 1, NOW(), NOW()),
-- 访问日志 (parent=1)
(1, '访问日志', 'access-log', 1, '/system/access-log', 'access-log:view', NULL, 5, 1, NOW(), NOW()),
-- 系统参数 (parent=1)
(1, '系统参数', 'sys-param', 1, '/system/config', 'sys-param:view', NULL, 6, 1, NOW(), NOW()),
(30, '更新参数', 'sys-param:update', 2, NULL, 'sys-param:update', NULL, 1, 1, NOW(), NOW()),
-- 操作日志 (parent=1)
(1, '操作日志', 'operation-log', 1, '/system/operation-log', 'operation-log:view', NULL, 7, 1, NOW(), NOW()),

-- ========== 版本管理子菜单 ==========
-- 主系统版本 (parent=2)
(2, '主系统版本', 'main-version', 1, '/version/main', 'main-version:view', NULL, 1, 1, NOW(), NOW()),
(33, '创建版本', 'main-version:create', 2, NULL, 'main-version:create', NULL, 1, 1, NOW(), NOW()),
(33, '编辑版本', 'main-version:update', 2, NULL, 'main-version:update', NULL, 2, 1, NOW(), NOW()),
(33, '删除版本', 'main-version:delete', 2, NULL, 'main-version:delete', NULL, 3, 1, NOW(), NOW()),
(33, '发布版本', 'main-version:publish', 2, NULL, 'main-version:publish', NULL, 4, 1, NOW(), NOW()),
(33, '下线版本', 'main-version:offline', 2, NULL, 'main-version:offline', NULL, 5, 1, NOW(), NOW()),
-- 插件版本 (parent=2)
(2, '插件版本', 'plugin-version', 1, '/version/plugin', 'plugin-version:view', NULL, 2, 1, NOW(), NOW()),
(39, '创建插件', 'plugin-version:create', 2, NULL, 'plugin-version:create', NULL, 1, 1, NOW(), NOW()),
(39, '编辑插件', 'plugin-version:update', 2, NULL, 'plugin-version:update', NULL, 2, 1, NOW(), NOW()),
(39, '删除插件', 'plugin-version:delete', 2, NULL, 'plugin-version:delete', NULL, 3, 1, NOW(), NOW()),
(39, '发布插件', 'plugin-version:publish', 2, NULL, 'plugin-version:publish', NULL, 4, 1, NOW(), NOW()),
(39, '下线插件', 'plugin-version:offline', 2, NULL, 'plugin-version:offline', NULL, 5, 1, NOW(), NOW()),

-- ========== 政策文件子菜单 (parent=3) ==========
(3, '上传文件', 'policy-file:create', 2, NULL, 'policy-file:create', NULL, 1, 1, NOW(), NOW()),
(3, '删除文件', 'policy-file:delete', 2, NULL, 'policy-file:delete', NULL, 2, 1, NOW(), NOW()),
(3, '启用/禁用', 'policy-file:update', 2, NULL, 'policy-file:update', NULL, 3, 1, NOW(), NOW()),

-- ========== 模型路由子菜单 (parent=6) ==========
(6, '创建规则', 'model-route:create', 2, NULL, 'model-route:create', NULL, 1, 1, NOW(), NOW()),
(6, '编辑规则', 'model-route:update', 2, NULL, 'model-route:update', NULL, 2, 1, NOW(), NOW()),
(6, '删除规则', 'model-route:delete', 2, NULL, 'model-route:delete', NULL, 3, 1, NOW(), NOW()),

-- ========== AI编制业务子菜单 (parent=7) ==========
(7, '项目管理', 'ai-project', 1, '/project', 'ai-project:view', 'icon-project', 1, 1, NOW(), NOW()),
(7, '需求管理', 'ai-requirement', 1, '/requirement', 'ai-requirement:view', 'icon-requirement', 2, 1, NOW(), NOW()),
(7, '评审项管理', 'ai-review', 1, '/review', 'ai-review:view', 'icon-review', 3, 1, NOW(), NOW()),
(7, '文档生成', 'ai-document', 1, '/document', 'ai-document:view', 'icon-document', 4, 1, NOW(), NOW()),
(7, 'AI助手', 'ai-assistant', 1, '/assistant', 'ai-assistant:view', 'icon-ai-assistant', 5, 1, NOW(), NOW()),
(7, '模板库', 'ai-template', 1, '/template', 'ai-template:view', 'icon-template', 6, 1, NOW(), NOW()),

-- ========== AI编制按钮权限 ==========
-- 项目
(51, '创建项目', 'ai-project:create', 2, NULL, 'ai-project:create', NULL, 1, 1, NOW(), NOW()),
(51, '编辑项目', 'ai-project:update', 2, NULL, 'ai-project:update', NULL, 2, 1, NOW(), NOW()),
(51, '删除项目', 'ai-project:delete', 2, NULL, 'ai-project:delete', NULL, 3, 1, NOW(), NOW()),
(51, '生成招标文件', 'ai-project:generate', 2, NULL, 'ai-project:generate', NULL, 4, 1, NOW(), NOW()),
(51, '发布项目', 'ai-project:publish', 2, NULL, 'ai-project:publish', NULL, 5, 1, NOW(), NOW()),
(51, '查看版本历史', 'ai-project:version', 2, NULL, 'ai-project:version', NULL, 6, 1, NOW(), NOW()),
(51, '导出文档', 'ai-project:export', 2, NULL, 'ai-project:export', NULL, 7, 1, NOW(), NOW()),
-- 需求
(52, '创建需求', 'ai-requirement:create', 2, NULL, 'ai-requirement:create', NULL, 1, 1, NOW(), NOW()),
(52, '编辑需求', 'ai-requirement:update', 2, NULL, 'ai-requirement:update', NULL, 2, 1, NOW(), NOW()),
(52, '删除需求', 'ai-requirement:delete', 2, NULL, 'ai-requirement:delete', NULL, 3, 1, NOW(), NOW()),
(52, 'AI生成需求', 'ai-requirement:ai-generate', 2, NULL, 'ai-requirement:ai-generate', NULL, 4, 1, NOW(), NOW()),
(52, '匹配历史模板', 'ai-requirement:match', 2, NULL, 'ai-requirement:match', NULL, 5, 1, NOW(), NOW()),
-- 评审项
(53, '创建评审项', 'ai-review:create', 2, NULL, 'ai-review:create', NULL, 1, 1, NOW(), NOW()),
(53, '编辑评审项', 'ai-review:update', 2, NULL, 'ai-review:update', NULL, 2, 1, NOW(), NOW()),
(53, '删除评审项', 'ai-review:delete', 2, NULL, 'ai-review:delete', NULL, 3, 1, NOW(), NOW()),
(53, 'AI生成评审项', 'ai-review:ai-generate', 2, NULL, 'ai-review:ai-generate', NULL, 4, 1, NOW(), NOW()),
(53, '导入评审项', 'ai-review:import', 2, NULL, 'ai-review:import', NULL, 5, 1, NOW(), NOW()),
-- 文档
(54, '创建文档', 'ai-document:create', 2, NULL, 'ai-document:create', NULL, 1, 1, NOW(), NOW()),
(54, 'AI生成文档', 'ai-document:generate', 2, NULL, 'ai-document:generate', NULL, 2, 1, NOW(), NOW()),
(54, '编辑文档', 'ai-document:update', 2, NULL, 'ai-document:update', NULL, 3, 1, NOW(), NOW()),
(54, '预览文档', 'ai-document:preview', 2, NULL, 'ai-document:preview', NULL, 4, 1, NOW(), NOW()),
(54, '导出文档', 'ai-document:export', 2, NULL, 'ai-document:export', NULL, 5, 1, NOW(), NOW()),
-- AI助手
(55, '使用AI助手', 'ai-assistant:chat', 2, NULL, 'ai-assistant:chat', NULL, 1, 1, NOW(), NOW()),
(55, '文本优化', 'ai-assistant:optimize', 2, NULL, 'ai-assistant:optimize', NULL, 2, 1, NOW(), NOW()),
-- 模板
(56, '使用模板', 'ai-template:use', 2, NULL, 'ai-template:use', NULL, 1, 1, NOW(), NOW()),
(56, '预览模板', 'ai-template:preview', 2, NULL, 'ai-template:preview', NULL, 2, 1, NOW(), NOW()),

-- ========== 新增一级菜单 ==========
(0, 'AI模型配置', 'model-config', 1, '/model-config', 'model-config:view', NULL, 7, 1, NOW(), NOW()),   -- id=83
(0, '模板管理', 'template-config', 1, '/template', 'template-config:view', NULL, 8, 1, NOW(), NOW()),   -- id=84
(0, '知识库管理', 'knowledge-config', 1, '/knowledge', 'knowledge-config:view', NULL, 9, 1, NOW(), NOW()), -- id=85

-- ========== AI模型配置子菜单 (parent=83) ==========
(83, '创建配置', 'model-config:create', 2, NULL, 'model-config:create', NULL, 1, 1, NOW(), NOW()),
(83, '编辑配置', 'model-config:update', 2, NULL, 'model-config:update', NULL, 2, 1, NOW(), NOW()),
(83, '删除配置', 'model-config:delete', 2, NULL, 'model-config:delete', NULL, 3, 1, NOW(), NOW()),

-- ========== 模板管理子菜单 (parent=84) ==========
(84, '创建模板', 'template-config:create', 2, NULL, 'template-config:create', NULL, 1, 1, NOW(), NOW()),
(84, '编辑模板', 'template-config:update', 2, NULL, 'template-config:update', NULL, 2, 1, NOW(), NOW()),
(84, '删除模板', 'template-config:delete', 2, NULL, 'template-config:delete', NULL, 3, 1, NOW(), NOW()),

-- ========== 知识库管理子菜单 (parent=85) ==========
(85, '创建知识库', 'knowledge-config:create', 2, NULL, 'knowledge-config:create', NULL, 1, 1, NOW(), NOW()),
(85, '编辑知识库', 'knowledge-config:update', 2, NULL, 'knowledge-config:update', NULL, 2, 1, NOW(), NOW()),
(85, '删除知识库', 'knowledge-config:delete', 2, NULL, 'knowledge-config:delete', NULL, 3, 1, NOW(), NOW());


-- =====================================================
-- 第四部分: 角色权限分配
-- =====================================================

-- 管理员角色: 分配所有菜单权限
INSERT INTO `sup_role_menu` (`role_id`, `menu_id`, `create_time`, `modify_time`)
SELECT 1, id, NOW(), NOW() FROM `sup_menu`;

-- 业务用户角色: 仅分配AI编制相关菜单权限
INSERT INTO `sup_role_menu` (`role_id`, `menu_id`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
SELECT r.id, m.id, NOW(), 1, 'admin', NOW(), 1, 'admin'
FROM `sup_role` r, `sup_menu` m
WHERE r.role_code = 'BID_USER'
  AND (m.menu_code LIKE 'ai-%' OR m.menu_code = 'ai-tender-biz');


-- =====================================================
-- 执行完成提示
-- =====================================================
SELECT '支撑中心初始化SQL执行完成!' AS message;
