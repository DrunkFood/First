package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI任务创建响应
 */
@Data
public class AiTaskCreateResponse implements Serializable {

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
     * 任务状态
     */
    private String status;

    /**
     * 创建时间(格式 yyyy-MM-dd HH:mm:ss)
     */
    private String createTime;
}
