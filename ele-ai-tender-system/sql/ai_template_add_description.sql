-- =========================================
-- ai_template 表新增 description 列
-- 用于模板描述信息展示
-- =========================================

ALTER TABLE ai_template
  ADD COLUMN description VARCHAR(500) DEFAULT NULL COMMENT '模板描述' AFTER project_type;
