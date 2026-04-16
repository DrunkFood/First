-- AI任务结果同步标记字段
-- 用于标识AI任务完成后，其结果是否已同步到对应的业务表
-- 三态: 0-未同步 1-已同步 2-同步失败(可重试)

ALTER TABLE ai_task ADD COLUMN `result_synced` TINYINT NOT NULL DEFAULT 0
    COMMENT '结果是否已同步到业务表: 0-未同步 1-已同步 2-同步失败'
    AFTER `timeout_minutes`;

CREATE INDEX idx_result_synced ON ai_task(`result_synced`, `status`);
