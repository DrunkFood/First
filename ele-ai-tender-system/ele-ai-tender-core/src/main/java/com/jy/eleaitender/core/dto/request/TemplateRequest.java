package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "模板创建/更新请求")
public class TemplateRequest {
    @NotBlank(message = "模板编码不能为空")
    @Schema(description = "模板编码")
    private String templateCode;
    
    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称")
    private String templateName;
    
    @NotBlank(message = "适用项目类别不能为空")
    @Schema(description = "适用项目类别")
    private String projectCategory;
    
    @NotBlank(message = "适用项目类型不能为空")
    @Schema(description = "适用项目类型")
    private String projectType;
    
    @NotBlank(message = "模板内容不能为空")
    @Schema(description = "模板内容(Markdown)")
    private String content;
    
    @Schema(description = "模板结构定义JSON")
    private String structureDefinition;
    
    @Schema(description = "状态")
    private String status;
}
