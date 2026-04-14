package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目阶段进度VO
 */
@Data
@Schema(description = "项目阶段进度")
public class ProjectPhaseVO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "当前阶段编号: 1-5")
    private Integer currentPhase;

    @Schema(description = "当前阶段名称")
    private String currentPhaseName;

    @Schema(description = "完成进度百分比: 0-100")
    private Integer progress;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;
}
