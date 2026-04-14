package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检测报告VO
 */
@Data
@Schema(description = "检测报告")
public class DetectionReportVO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "总体状态")
    private String overallStatus;

    @Schema(description = "总问题数")
    private Integer totalIssueCount;

    @Schema(description = "总得分")
    private BigDecimal totalScore;

    @Schema(description = "问题列表")
    private List<DetectionIssueVO> issues;
}
