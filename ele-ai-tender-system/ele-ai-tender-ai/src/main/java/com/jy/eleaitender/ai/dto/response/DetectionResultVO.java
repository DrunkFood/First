package com.jy.eleaitender.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "检测结果")
public class DetectionResultVO {
    @Schema(description = "检测记录ID")
    private Long id;
    @Schema(description = "检测类型")
    private String detectionType;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "检测结果JSON")
    private String result;
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
}
