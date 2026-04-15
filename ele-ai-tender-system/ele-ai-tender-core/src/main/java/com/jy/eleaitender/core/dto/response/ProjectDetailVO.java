package com.jy.eleaitender.core.dto.response;

import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "项目详情")
public class ProjectDetailVO {
    @Schema(description = "项目信息")
    private AiProject project;
    @Schema(description = "关联的业务需求")
    private AiRequirement requirement;
}
