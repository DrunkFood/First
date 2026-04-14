package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 提交检测请求
 */
@Data
@Schema(description = "提交检测请求")
public class DetectionSubmitRequest {

    @Schema(description = "选中的政策文件ID列表")
    private List<Long> policyFileIds;
}
