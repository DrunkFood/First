package com.jy.eleaitender.core.dto.response;

import com.jy.eleaitender.common.entity.ai.AiTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "模板详情")
public class TemplateDetailVO {
    @Schema(description = "模板信息")
    private AiTemplate template;
}
