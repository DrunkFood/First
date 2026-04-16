package com.jy.eleaitender.common.entity.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI任务实体
 * 对应表: ai_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_task")
@Schema(description = "AI任务")
public class AiTask extends BaseEntity {

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联业务ID")
    private Long bizId;

    @Schema(description = "业务类型: REQUIREMENT/PROJECT/DETECTION")
    private String bizType;

    @Schema(description = "请求参数(JSON)")
    private String requestParams;

    @Schema(description = "关联文件ID列表(逗号分隔)")
    private String fileIds;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "执行结果(JSON)")
    private String result;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @Schema(description = "AI开始处理时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "超时时间(分钟)")
    private Integer timeoutMinutes;

    @Schema(description = "结果是否已同步到业务表: 0-未同步 1-已同步 2-同步失败")
    private Integer resultSynced;
}
