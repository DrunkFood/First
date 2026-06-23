package com.jy.eleaitender.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模型连通性测试响应。
 */
@Data
@Schema(description = "模型连通性测试响应")
public class ModelConnectivityTestResponse {

    @Schema(description = "模型供应商")
    private String provider;

    @Schema(description = "模型配置ID")
    private Long modelConfigId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "API端点URL")
    private String apiEndpoint;

    @Schema(description = "是否调用成功")
    private boolean success;

    @Schema(description = "AI返回内容")
    private String content;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "耗时，毫秒")
    private long elapsedMs;
}
