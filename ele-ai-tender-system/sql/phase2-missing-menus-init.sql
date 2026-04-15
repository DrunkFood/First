-- =====================================================
-- 增量脚本: 补充支撑中心缺失的顶级菜单
- AI模型配置、模板管理、知识库管理
-- 数据库: ele_ai_tender
-- 执行时机: 在 init.sql 之后执行，或直接在现有库上执行
-- =====================================================

USE `ele_ai_tender`;

-- ========== 新增一级菜单 ==========
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `icon`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(0, 'AI模型配置', 'model-config', 1, '/model-config', 'model-config:view', NULL, 7, 1, NOW(), NOW()),
(0, '模板管理', 'template-config', 1, '/template', 'template-config:view', NULL, 8, 1, NOW(), NOW()),
(0, '知识库管理', 'knowledge-config', 1, '/knowledge', 'knowledge-config:view', NULL, 9, 1, NOW(), NOW());

-- 获取刚插入的一级菜单ID
SET @model_config_menu_id = (SELECT id FROM `sup_menu` WHERE `menu_code` = 'model-config' AND `is_delete` = 0 LIMIT 1);
SET @template_config_menu_id = (SELECT id FROM `sup_menu` WHERE `menu_code` = 'template-config' AND `is_delete` = 0 LIMIT 1);
SET @knowledge_config_menu_id = (SELECT id FROM `sup_menu` WHERE `menu_code` = 'knowledge-config' AND `is_delete` = 0 LIMIT 1);

-- ========== AI模型配置按钮权限 ==========
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `icon`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@model_config_menu_id, '创建配置', 'model-config:create', 2, NULL, 'model-config:create', NULL, 1, 1, NOW(), NOW()),
(@model_config_menu_id, '编辑配置', 'model-config:update', 2, NULL, 'model-config:update', NULL, 2, 1, NOW(), NOW()),
(@model_config_menu_id, '删除配置', 'model-config:delete', 2, NULL, 'model-config:delete', NULL, 3, 1, NOW(), NOW());

-- ========== 模板管理按钮权限 ==========
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `icon`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@template_config_menu_id, '创建模板', 'template-config:create', 2, NULL, 'template-config:create', NULL, 1, 1, NOW(), NOW()),
(@template_config_menu_id, '编辑模板', 'template-config:update', 2, NULL, 'template-config:update', NULL, 2, 1, NOW(), NOW()),
(@template_config_menu_id, '删除模板', 'template-config:delete', 2, NULL, 'template-config:delete', NULL, 3, 1, NOW(), NOW());

-- ========== 知识库管理按钮权限 ==========
INSERT INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `icon`, `sort_order`, `status`, `create_time`, `modify_time`) VALUES
(@knowledge_config_menu_id, '创建知识库', 'knowledge-config:create', 2, NULL, 'knowledge-config:create', NULL, 1, 1, NOW(), NOW()),
(@knowledge_config_menu_id, '编辑知识库', 'knowledge-config:update', 2, NULL, 'knowledge-config:update', NULL, 2, 1, NOW(), NOW()),
(@knowledge_config_menu_id, '删除知识库', 'knowledge-config:delete', 2, NULL, 'knowledge-config:delete', NULL, 3, 1, NOW(), NOW());

-- ========== 管理员角色分配新增菜单权限 ==========
INSERT INTO `sup_role_menu` (`role_id`, `menu_id`, `create_time`, `modify_time`)
SELECT 1, id, NOW(), NOW() FROM `sup_menu`
WHERE `menu_code` IN (
    'model-config', 'model-config:create', 'model-config:update', 'model-config:delete',
    'template-config', 'template-config:create', 'template-config:update', 'template-config:delete',
    'knowledge-config', 'knowledge-config:create', 'knowledge-config:update', 'knowledge-config:delete'
) AND `is_delete` = 0;

-- 执行完成提示
SELECT '缺失菜单补充完成!' AS message;
