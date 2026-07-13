package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * AI任务状态响应VO
 */
@Data
@Schema(description = "AI任务状态")
public class AiTaskVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务类型名称")
    private String taskTypeName;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联业务ID")
    private Long bizId;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "执行结果(JSON)")
    private String result;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @Schema(description = "AI开始处理时间")
    private Date startedAt;

    @Schema(description = "完成时间")
    private Date completedAt;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "结果是否已同步到业务表: 0-未同步 1-已同步 2-同步失败 3-同步中")
    private Integer resultSynced;
}
