-- AI响应记录表
-- 记录每次AI模型调用的详细信息：请求内容、响应内容、token消耗等
CREATE TABLE IF NOT EXISTS `ai_response_log` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `model`             VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '模型名称',
    `role`              VARCHAR(16)  NOT NULL COMMENT '对话角色: GENERATION/OPTIMIZATION/DETECTION/CHAT',
    `messages`          LONGTEXT     NOT NULL COMMENT '对话内容messages(JSON)',
    `content`           LONGTEXT     NOT NULL COMMENT 'AI响应内容',
    `finish_reason`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '完成原因: stop/length/tool_calls等',
    `prompt_tokens`     INT          NOT NULL DEFAULT 0 COMMENT '包含历史问题的总tokens大小',
    `completion_tokens` INT          NOT NULL DEFAULT 0 COMMENT '回答的tokens大小',
    `total_tokens`      INT          NOT NULL DEFAULT 0 COMMENT '本次交互计费的tokens大小',
    `task_id`           BIGINT       DEFAULT NULL COMMENT '关联AI任务ID(ai_task.id), 对话类调用为NULL',
    `conversation_id`   VARCHAR(64)  DEFAULT NULL COMMENT '对话ID, 发起AI聊天时随机生成的UUID',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID(任务类调用取ai_task.create_id)',
    `create_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`               INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_role` (`role`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_model` (`model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI响应记录表';
