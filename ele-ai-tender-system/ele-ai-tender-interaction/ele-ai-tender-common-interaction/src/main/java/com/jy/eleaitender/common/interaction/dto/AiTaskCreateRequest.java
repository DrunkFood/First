package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI任务创建请求
 */
@Data
public class AiTaskCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务类型，取值如 REQUIREMENT_GENERATE / DETECTION_SENSITIVE_WORD 等
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
     * 请求参数(JSON字符串)
     */
    private String requestParams;

    /**
     * 关联文件ID列表(逗号分隔)
     */
    private String fileIds;
}
