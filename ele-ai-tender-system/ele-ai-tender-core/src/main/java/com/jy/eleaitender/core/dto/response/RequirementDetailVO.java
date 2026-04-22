package com.jy.eleaitender.core.dto.response;

import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.common.entity.support.SupTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "需求详情")
public class RequirementDetailVO {
    @Schema(description = "需求信息")
    private AiRequirement requirement;
    @Schema(description = "匹配的模板")
    private SupTemplate matchedTemplate;
}
