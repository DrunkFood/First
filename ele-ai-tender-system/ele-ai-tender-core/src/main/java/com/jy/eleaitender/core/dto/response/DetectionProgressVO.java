package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检测进度VO
 */
@Data
@Schema(description = "检测进度")
public class DetectionProgressVO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "总体状态: DETECTING/PASSED/FAILED/SKIPPED")
    private String overallStatus;

    @Schema(description = "各检测项进度")
    private List<DetectionItemProgress> items;

    @Data
    @Schema(description = "检测项进度")
    public static class DetectionItemProgress {

        @Schema(description = "检测类型: SENSITIVE_WORD/TYPO/POLICY_REVIEW/FORMAT_CHECK")
        private String detectionType;

        @Schema(description = "检测类型中文名")
        private String typeName;

        @Schema(description = "任务状态: PENDING/PROCESSING/COMPLETED/FAILED/AI_UNAVAILABLE")
        private String taskStatus;

        @Schema(description = "问题数")
        private Integer issueCount;

        @Schema(description = "得分")
        private BigDecimal score;
    }
}
