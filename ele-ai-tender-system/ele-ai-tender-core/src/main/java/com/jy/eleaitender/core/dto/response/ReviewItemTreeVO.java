package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "评审项树节点")
public class ReviewItemTreeVO {
    @Schema(description = "评审项ID")
    private Long id;
    @Schema(description = "父级ID")
    private Long parentId;
    @Schema(description = "层级")
    private Integer level;
    @Schema(description = "评审项名称")
    private String itemName;
    @Schema(description = "评审项内容")
    private String itemContent;
    @Schema(description = "排序号")
    private Integer sortOrder;
    @Schema(description = "子节点")
    private List<ReviewItemTreeVO> children;
}
