package com.jy.eleaitender.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 外部用户信息响应DTO
 */
@Data
@Schema(description = "外部用户信息响应")
public class ExternalUserInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "应用Key")
    private String appKey;

    @Schema(description = "外部用户ID")
    private String userId;

    @Schema(description = "外部用户名称")
    private String userName;

    @Schema(description = "企业ID")
    private String enterpriseId;

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "企业社会统一信用代码")
    private String enterpriseCode;
}
