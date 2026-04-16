-- AI内容反馈表
CREATE TABLE IF NOT EXISTS `ai_content_feedback` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `task_id`         BIGINT       DEFAULT NULL COMMENT '关联AI任务ID(ai_task.id), 聊天反馈无任务时为NULL',
    `feedback_type`   VARCHAR(20)  NOT NULL COMMENT '反馈类型: LIKE/DISLIKE',
    `feedback_scene`  VARCHAR(30)  NOT NULL COMMENT '反馈场景: GENERATION_CONTENT/CHAT_MESSAGE',
    `chat_message_id` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '聊天消息标识, 生成内容反馈时为空字符串',
    `chat_content`    TEXT         DEFAULT NULL COMMENT '被反馈的AI聊天消息内容摘要(最多200字)',
    `reason`          VARCHAR(500) DEFAULT NULL COMMENT '不满意原因(DISLIKE时可选填)',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`             INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `is_delete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_target` (`create_id`, `task_id`, `feedback_scene`, `chat_message_id`, `is_delete`),
    INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI内容反馈表';
