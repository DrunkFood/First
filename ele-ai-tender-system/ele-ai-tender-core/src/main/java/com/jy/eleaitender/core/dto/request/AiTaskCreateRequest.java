package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建AI任务请求DTO
 */
@Data
@Schema(description = "创建AI任务请求")
public class AiTaskCreateRequest {

    @Schema(description = "任务类型", required = true)
    private String taskType;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联业务ID")
    private Long bizId;

    @Schema(description = "业务类型: REQUIREMENT/REVIEW_ITEM/DETECTION")
    private String bizType;

    @Schema(description = "请求参数(JSON)")
    private String requestParams;

    @Schema(description = "关联文件ID列表(逗号分隔)")
    private String fileIds;
}
