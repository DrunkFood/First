-- =====================================================
-- Phase1: 支撑中心功能增强 SQL脚本
-- 包含: 系统参数表、政策文件表、模型路由规则表
-- 注意: sup_message 和 sup_operation_log 已在其他脚本中创建
-- =====================================================

-- =========================================
-- 1. 系统参数表
-- =========================================
CREATE TABLE IF NOT EXISTS `sup_sys_parameter` (
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

-- 初始化系统参数数据
INSERT INTO `sup_sys_parameter` (`param_group`, `param_key`, `param_value`, `param_type`, `param_name`, `description`, `sort_order`, `create_time`, `modify_time`) VALUES
('SYSTEM', 'system_name', '招标文件AI编制工具', 'STRING', '系统名称', '系统显示名称', 1, NOW(), NOW()),
('SYSTEM', 'max_file_size_mb', '50', 'NUMBER', '最大文件大小(MB)', '上传文件大小限制，单位MB', 2, NOW(), NOW()),
('SYSTEM', 'session_timeout_min', '120', 'NUMBER', '会话超时时间(分钟)', '用户会话无操作超时时间', 3, NOW(), NOW()),
('SYSTEM', 'detection_fail_limit', '3', 'NUMBER', '检测不通过上限次数', '项目检测不通过的最大允许次数', 4, NOW(), NOW()),
('SWITCH', 'enable_ai_chat', 'true', 'BOOLEAN', '启用AI对话功能', '开启后用户可使用浮动AI对话组件', 1, NOW(), NOW()),
('SWITCH', 'enable_detection', 'true', 'BOOLEAN', '启用智能检测功能', '开启后系统将自动进行文档检测', 2, NOW(), NOW()),
('SWITCH', 'enable_notification', 'true', 'BOOLEAN', '启用消息通知', '开启后用户将收到系统消息通知', 3, NOW(), NOW()),
('AI_RULE', 'fairness_rules', '检查是否指定特定品牌或供应商，是否设置不合理门槛，是否存在地域限制等内容。', 'JSON', '公平竞争检测规则', 'AI公平竞争检测使用的规则文本', 1, NOW(), NOW()),
('AI_RULE', 'compliance_rules', '检查是否符合相关法律法规要求，是否符合招标文件格式规范，是否存在遗漏必要条款等内容。', 'JSON', '合规性检查规则', 'AI合规性检查使用的规则文本', 2, NOW(), NOW());

-- =========================================
-- 2. 系统政策文件表
-- =========================================
CREATE TABLE IF NOT EXISTS `sup_policy_file` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name`             VARCHAR(200) NOT NULL COMMENT '文件名称',
    `file_category`         VARCHAR(30)  NOT NULL COMMENT '文件分类: LAW(法律法规)/REGULATION(规章制度)/POLICY(政策文件)',
    `applicable_category`   VARCHAR(30)  DEFAULT NULL COMMENT '适用项目类别: LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT',
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

-- =========================================
-- 3. 模型路由规则表
-- =========================================
CREATE TABLE IF NOT EXISTS `sup_model_route_rule` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `usage_scenario`    VARCHAR(50)  NOT NULL COMMENT '使用场景: GENERATION/OPTIMIZATION/DETECTION',
    `primary_model_id`  BIGINT       NOT NULL COMMENT '优先模型ID(关联ai_model_config.id)',
    `fallback_model_id` BIGINT       DEFAULT NULL COMMENT '降级模型ID(关联ai_model_config.id)',
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

-- =========================================
-- 4. 新增菜单数据
-- =========================================

-- 获取"系统管理"菜单ID
SET @system_menu_id = (SELECT id FROM `sup_menu` WHERE menu_code = 'system' AND is_delete = 0 LIMIT 1);

-- 系统参数 (挂在系统管理下)
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@system_menu_id, '系统参数', 'sys-param', 1, '/system/config', 'sys-param:view', 6, 1, NOW(), NOW());
SET @sys_param_menu_id = LAST_INSERT_ID();
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@sys_param_menu_id, '更新参数', 'sys-param:update', 2, NULL, 'sys-param:update', 1, 1, NOW(), NOW());

-- 操作日志 (挂在系统管理下)
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@system_menu_id, '操作日志', 'operation-log', 1, '/system/operation-log', 'operation-log:view', 7, 1, NOW(), NOW());

-- 政策文件管理 (一级菜单)
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(0, '政策文件', 'policy-file', 1, '/policy-file', 'policy-file:view', 3, 1, NOW(), NOW());
SET @policy_menu_id = LAST_INSERT_ID();
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@policy_menu_id, '上传文件', 'policy-file:create', 2, NULL, 'policy-file:create', 1, 1, NOW(), NOW()),
(@policy_menu_id, '删除文件', 'policy-file:delete', 2, NULL, 'policy-file:delete', 2, 1, NOW(), NOW()),
(@policy_menu_id, '启用/禁用', 'policy-file:update', 2, NULL, 'policy-file:update', 3, 1, NOW(), NOW());

-- 消息中心 (一级菜单)
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(0, '消息中心', 'message', 1, '/message', NULL, 4, 1, NOW(), NOW());

-- 统计分析 (一级菜单)
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(0, '统计分析', 'statistics', 1, '/statistics', NULL, 5, 1, NOW(), NOW());

-- 模型路由 (一级菜单)
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(0, '模型路由', 'model-route', 1, '/model-route', 'model-route:view', 6, 1, NOW(), NOW());
SET @route_menu_id = LAST_INSERT_ID();
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@route_menu_id, '创建规则', 'model-route:create', 2, NULL, 'model-route:create', 1, 1, NOW(), NOW()),
(@route_menu_id, '编辑规则', 'model-route:update', 2, NULL, 'model-route:update', 2, 1, NOW(), NOW()),
(@route_menu_id, '删除规则', 'model-route:delete', 2, NULL, 'model-route:delete', 3, 1, NOW(), NOW());

-- =========================================
-- 5. 给管理员角色分配新增菜单权限
-- =========================================
INSERT IGNORE INTO `sup_role_menu` (`role_id`, `menu_id`, `create_time`, `modify_time`)
SELECT 1, m.id, NOW(), NOW()
FROM `sup_menu` m
LEFT JOIN `sup_role_menu` rm ON rm.role_id = 1 AND rm.menu_id = m.id
WHERE rm.id IS NULL;

-- =========================================
-- 执行完成提示
-- =========================================
SELECT 'Phase1 支撑中心增强SQL执行完成!' AS message;
