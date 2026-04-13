package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "需求模板匹配请求")
public class RequirementMatchRequest {
    @Schema(description = "匹配模式")
    private String matchMode;
    
    @Schema(description = "匹配的文件ID")
    private Long matchedFileId;
    
    @Schema(description = "上传的文件ID")
    private Long uploadedFileId;
}
