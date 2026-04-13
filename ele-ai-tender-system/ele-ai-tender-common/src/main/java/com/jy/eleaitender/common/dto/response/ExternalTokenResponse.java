package com.jy.eleaitender.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 外部系统Token响应DTO
 */
@Data
@Schema(description = "外部系统Token响应")
public class ExternalTokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Token")
    private String token;

    @Schema(description = "过期时间（秒）")
    private Long expireIn;

    @Schema(description = "Token类型")
    private String tokenType = "Bearer";

    public ExternalTokenResponse() {}

    public ExternalTokenResponse(String token, Long expireIn) {
        this.token = token;
        this.expireIn = expireIn;
    }
}
