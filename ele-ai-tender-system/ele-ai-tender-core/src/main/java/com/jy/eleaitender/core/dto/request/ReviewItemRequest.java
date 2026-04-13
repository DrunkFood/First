package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "评审项创建/更新请求")
public class ReviewItemRequest {
    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private Long projectId;
    
    @Schema(description = "父级ID")
    private Long parentId = 0L;
    
    @Schema(description = "层级")
    private Integer level = 1;
    
    @NotBlank(message = "评审项名称不能为空")
    @Schema(description = "评审项名称")
    private String itemName;
    
    @Schema(description = "评审项内容")
    private String itemContent;
    
    @Schema(description = "排序号")
    private Integer sortOrder = 0;
}
