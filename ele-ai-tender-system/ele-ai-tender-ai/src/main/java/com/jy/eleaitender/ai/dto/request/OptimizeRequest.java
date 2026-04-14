package com.jy.eleaitender.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文本优化请求DTO
 */
@Data
@Schema(description = "文本优化请求")
public class OptimizeRequest {

    @NotBlank(message = "待优化内容不能为空")
    @Schema(description = "待优化的文本内容")
    private String content;

    @Schema(description = "优化要求说明")
    private String requirement;

    @Schema(description = "关联项目ID")
    private Long projectId;
}
