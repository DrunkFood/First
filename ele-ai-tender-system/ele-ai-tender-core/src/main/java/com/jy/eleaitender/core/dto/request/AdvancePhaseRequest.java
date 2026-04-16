package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 阶段推进请求
 */
@Data
@Schema(description = "阶段推进请求")
public class AdvancePhaseRequest {

    @Schema(description = "目标阶段编号: 1-5")
    private Integer targetPhase;

    @Schema(description = "上下文参数（如policyFileIds等）")
    private Map<String, Object> context;
}
