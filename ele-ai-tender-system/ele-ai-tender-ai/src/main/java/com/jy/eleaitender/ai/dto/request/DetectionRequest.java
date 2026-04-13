package com.jy.eleaitender.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "检测启动请求")
public class DetectionRequest {
    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private Long projectId;
    @Schema(description = "检测类型")
    private String detectionType;
    @Schema(description = "检测内容快照")
    private String contentSnapshot;
}
