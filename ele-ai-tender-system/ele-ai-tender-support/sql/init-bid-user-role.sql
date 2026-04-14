-- =========================================
-- AI编制系统业务用户角色初始化脚本
-- 用于创建普通业务用户角色，仅允许访问AI编制系统
-- =========================================

USE `ele_ai_tender`;

-- =========================================
-- 1. 创建业务用户角色
-- =========================================
INSERT INTO `sup_role` (`role_code`, `role_name`, `description`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`) 
VALUES ('BID_USER', '业务用户', 'AI招标文件编制业务用户，仅可访问AI编制系统，不可访问支撑系统', 1, NOW(), 1, 'admin', NOW(), 1, 'admin');

-- =========================================
-- 2. 创建AI编制系统专属菜单（如果不存在）
-- =========================================
-- 注意：这些菜单已在支撑系统init.sql中创建，这里仅创建补充菜单

-- AI编制业务菜单（一级菜单，如果不存在则创建）
INSERT IGNORE INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `sort_order`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`) 
VALUES 
(0, 'AI招标文件编制', 'ai-tender-biz', 1, '/ai-tender', NULL, 10, 1, NOW(), 1, 'admin', NOW(), 1, 'admin');

-- 获取AI编制业务菜单ID
SET @ai_tender_biz_menu_id = (SELECT id FROM sup_menu WHERE menu_code = 'ai-tender-biz' AND parent_id = 0 LIMIT 1);

-- 创建AI编制业务子菜单
INSERT IGNORE INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `icon`, `sort_order`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
VALUES
-- 项目管理工作流
(@ai_tender_biz_menu_id, '项目管理', 'ai-project', 1, '/project', 'ai-project:view', 'icon-project', 1, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_tender_biz_menu_id, '需求管理', 'ai-requirement', 1, '/requirement', 'ai-requirement:view', 'icon-requirement', 2, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_tender_biz_menu_id, '评审项管理', 'ai-review', 1, '/review', 'ai-review:view', 'icon-review', 3, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_tender_biz_menu_id, '文档生成', 'ai-document', 1, '/document', 'ai-document:view', 'icon-document', 4, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_tender_biz_menu_id, 'AI助手', 'ai-assistant', 1, '/assistant', 'ai-assistant:view', 'icon-ai-assistant', 5, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_tender_biz_menu_id, '模板库', 'ai-template', 1, '/template', 'ai-template:view', 'icon-template', 6, 1, NOW(), 1, 'admin', NOW(), 1, 'admin');

-- 获取所有AI编制业务菜单ID
SET @ai_project_menu_id = (SELECT id FROM sup_menu WHERE menu_code = 'ai-project' AND menu_url = '/project' LIMIT 1);
SET @ai_requirement_menu_id = (SELECT id FROM sup_menu WHERE menu_code = 'ai-requirement' AND menu_url = '/requirement' LIMIT 1);
SET @ai_review_menu_id = (SELECT id FROM sup_menu WHERE menu_code = 'ai-review' AND menu_url = '/review' LIMIT 1);
SET @ai_document_menu_id = (SELECT id FROM sup_menu WHERE menu_code = 'ai-document' AND menu_url = '/document' LIMIT 1);
SET @ai_assistant_menu_id = (SELECT id FROM sup_menu WHERE menu_code = 'ai-assistant' AND menu_url = '/assistant' LIMIT 1);
SET @ai_template_menu_id = (SELECT id FROM sup_menu WHERE menu_code = 'ai-template' AND menu_url = '/template' LIMIT 1);

-- 创建按钮级权限
INSERT IGNORE INTO `sup_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `menu_url`, `permission`, `icon`, `sort_order`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
VALUES 
-- 项目按钮权限
(@ai_project_menu_id, '创建项目', 'ai-project:create', 2, NULL, 'ai-project:create', NULL, 1, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_project_menu_id, '编辑项目', 'ai-project:update', 2, NULL, 'ai-project:update', NULL, 2, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_project_menu_id, '删除项目', 'ai-project:delete', 2, NULL, 'ai-project:delete', NULL, 3, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_project_menu_id, '生成招标文件', 'ai-project:generate', 2, NULL, 'ai-project:generate', NULL, 4, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_project_menu_id, '发布项目', 'ai-project:publish', 2, NULL, 'ai-project:publish', NULL, 5, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_project_menu_id, '查看版本历史', 'ai-project:version', 2, NULL, 'ai-project:version', NULL, 6, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_project_menu_id, '导出文档', 'ai-project:export', 2, NULL, 'ai-project:export', NULL, 7, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),

-- 需求按钮权限
(@ai_requirement_menu_id, '创建需求', 'ai-requirement:create', 2, NULL, 'ai-requirement:create', NULL, 1, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_requirement_menu_id, '编辑需求', 'ai-requirement:update', 2, NULL, 'ai-requirement:update', NULL, 2, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_requirement_menu_id, '删除需求', 'ai-requirement:delete', 2, NULL, 'ai-requirement:delete', NULL, 3, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_requirement_menu_id, 'AI生成需求', 'ai-requirement:ai-generate', 2, NULL, 'ai-requirement:ai-generate', NULL, 4, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_requirement_menu_id, '匹配历史模板', 'ai-requirement:match', 2, NULL, 'ai-requirement:match', NULL, 5, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),

-- 评审项按钮权限
(@ai_review_menu_id, '创建评审项', 'ai-review:create', 2, NULL, 'ai-review:create', NULL, 1, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_review_menu_id, '编辑评审项', 'ai-review:update', 2, NULL, 'ai-review:update', NULL, 2, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_review_menu_id, '删除评审项', 'ai-review:delete', 2, NULL, 'ai-review:delete', NULL, 3, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_review_menu_id, 'AI生成评审项', 'ai-review:ai-generate', 2, NULL, 'ai-review:ai-generate', NULL, 4, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_review_menu_id, '导入评审项', 'ai-review:import', 2, NULL, 'ai-review:import', NULL, 5, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),

-- 文档按钮权限
(@ai_document_menu_id, '创建文档', 'ai-document:create', 2, NULL, 'ai-document:create', NULL, 1, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_document_menu_id, 'AI生成文档', 'ai-document:generate', 2, NULL, 'ai-document:generate', NULL, 2, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_document_menu_id, '编辑文档', 'ai-document:update', 2, NULL, 'ai-document:update', NULL, 3, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_document_menu_id, '预览文档', 'ai-document:preview', 2, NULL, 'ai-document:preview', NULL, 4, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_document_menu_id, '导出文档', 'ai-document:export', 2, NULL, 'ai-document:export', NULL, 5, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),

-- AI助手按钮权限
(@ai_assistant_menu_id, '使用AI助手', 'ai-assistant:chat', 2, NULL, 'ai-assistant:chat', NULL, 1, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_assistant_menu_id, '文本优化', 'ai-assistant:optimize', 2, NULL, 'ai-assistant:optimize', NULL, 2, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),

-- 模板按钮权限
(@ai_template_menu_id, '使用模板', 'ai-template:use', 2, NULL, 'ai-template:use', NULL, 1, 1, NOW(), 1, 'admin', NOW(), 1, 'admin'),
(@ai_template_menu_id, '预览模板', 'ai-template:preview', 2, NULL, 'ai-template:preview', NULL, 2, 1, NOW(), 1, 'admin', NOW(), 1, 'admin');

-- =========================================
-- 3. 给业务用户角色分配AI编制系统菜单权限
-- =========================================
-- 获取业务用户角色ID
SET @bid_user_role_id = (SELECT id FROM sup_role WHERE role_code = 'BID_USER' LIMIT 1);

-- 删除旧的权限关联(避免重复)
DELETE FROM `sup_role_menu` 
WHERE role_id = @bid_user_role_id 
  AND menu_id IN (
    SELECT id FROM (
      SELECT id FROM `sup_menu` 
      WHERE menu_code LIKE 'ai-%' OR menu_code = 'ai-tender-biz'
    ) tmp
  );

-- 分配所有AI编制相关菜单权限(不包括支撑系统菜单)
INSERT INTO `sup_role_menu` (`role_id`, `menu_id`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`)
SELECT @bid_user_role_id, id, NOW(), 1, 'admin', NOW(), 1, 'admin' 
FROM `sup_menu` 
WHERE menu_code LIKE 'ai-%' OR menu_code = 'ai-tender-biz';

-- =========================================
-- 4. 创建测试业务用户（可选）
-- =========================================
-- 创建测试用户(密码:user123,BCrypt加密:$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS)
INSERT IGNORE INTO `sup_user` (`username`, `password`, `real_name`, `phone`, `email`, `status`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`) 
VALUES ('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', '测试用户', '13800138000', 'testuser@example.com', 1, NOW(), 1, 'admin', NOW(), 1, 'admin');

-- 关联测试用户和业务用户角色
SET @testuser_id = (SELECT id FROM sup_user WHERE username = 'testuser' LIMIT 1);
SET @bid_user_role_id = (SELECT id FROM sup_role WHERE role_code = 'BID_USER' LIMIT 1);

INSERT IGNORE INTO `sup_user_role` (`user_id`, `role_id`, `create_time`, `create_id`, `create_name`, `modify_time`, `modify_id`, `modify_name`) 
VALUES (@testuser_id, @bid_user_role_id, NOW(), 1, 'admin', NOW(), 1, 'admin');

-- =========================================
-- 验证
-- =========================================
-- 查看业务用户角色
SELECT '业务用户角色:' AS info;
SELECT * FROM sup_role WHERE role_code = 'BID_USER';

-- 查看业务用户菜单权限数量
SELECT '业务用户菜单权限数量:' AS info;
SELECT COUNT(*) AS menu_count 
FROM sup_role_menu rm 
JOIN sup_role r ON rm.role_id = r.id 
WHERE r.role_code = 'BID_USER';

-- 查看测试用户
SELECT '测试业务用户:' AS info;
SELECT id, username, real_name, phone, status FROM sup_user WHERE username = 'testuser';

-- 查看测试用户角色
SELECT '测试用户角色关联:' AS info;
SELECT u.username, r.role_code, r.role_name 
FROM sup_user u
JOIN sup_user_role ur ON u.id = ur.user_id
JOIN sup_role r ON ur.role_id = r.id
WHERE u.username = 'testuser';

SELECT '========== AI编制系统菜单列表 ==========' AS info;
SELECT id, parent_id, menu_name, menu_code, menu_type, menu_url, permission, sort_order 
FROM sup_menu 
WHERE menu_code LIKE 'ai-%' OR menu_code = 'ai-tender-biz'
ORDER BY parent_id, sort_order;
