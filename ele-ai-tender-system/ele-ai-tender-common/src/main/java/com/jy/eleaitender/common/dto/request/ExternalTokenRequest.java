package com.jy.eleaitender.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 外部系统Token请求DTO
 */
@Data
@Schema(description = "外部系统Token请求")
public class ExternalTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名称不能为空")
    @Schema(description = "外部用户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "外部用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @NotBlank(message = "企业名称不能为空")
    @Schema(description = "企业名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String enterpriseName;

    @NotBlank(message = "企业ID不能为空")
    @Schema(description = "企业ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String enterpriseId;

    @NotBlank(message = "企业社会统一信用代码不能为空")
    @Schema(description = "企业社会统一信用代码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String enterpriseCode;

}
