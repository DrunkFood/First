package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI任务查询响应
 */
@Data
public class AiTaskQueryResponse implements Serializable {

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
     * 任务状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 执行结果(JSON)
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetry;

    /**
     * AI开始处理时间(格式 yyyy-MM-dd HH:mm:ss)
     */
    private String startedAt;

    /**
     * 完成时间(格式 yyyy-MM-dd HH:mm:ss)
     */
    private String completedAt;

    /**
     * 创建时间(格式 yyyy-MM-dd HH:mm:ss)
     */
    private String createTime;
}
