-- =========================================
-- ai_detection_record 表新增 requirement_id 列
-- 用于支持需求级检测记录（区分项目级/需求级检测）
-- =========================================

ALTER TABLE ai_detection_record
  ADD COLUMN requirement_id BIGINT DEFAULT NULL COMMENT '关联的业务需求ID(需求级检测时非空)' AFTER id;

CREATE INDEX idx_requirement ON ai_detection_record(requirement_id);
