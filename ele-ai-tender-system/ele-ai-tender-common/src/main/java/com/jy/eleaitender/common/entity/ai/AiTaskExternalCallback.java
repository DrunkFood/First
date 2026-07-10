package com.jy.eleaitender.common.entity.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI任务外部回调记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_task_external_callback")
@Schema(description = "AI任务外部回调记录")
public class AiTaskExternalCallback extends BaseEntity {

    @Schema(description = "AI任务ID")
    private Long taskId;

    @Schema(description = "外部系统appKey")
    private String appKey;

    @Schema(description = "回调状态: PENDING/PROCESSING/SUCCESS/FAILED")
    private String callbackStatus;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "最后回调时间")
    private LocalDateTime lastCallbackTime;

    @Schema(description = "错误信息")
    private String errorMsg;
}
