package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI任务终态结果回调请求
 */
@Data
public class AiTaskResultCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 业务ID
     */
    private String bizId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 任务状态(COMPLETED/FAILED/AI_UNAVAILABLE/SKIPPED)
     */
    private String status;

    /**
     * 执行结果(JSON)
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 完成时间(格式 yyyy-MM-dd HH:mm:ss)
     */
    private String completedAt;
}
