package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 外部系统 Token 响应
 */
@Data
public class ExternalTokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private Long expireIn;
    private String tokenType = "Bearer";

    public ExternalTokenResponse() {
    }

    public ExternalTokenResponse(String token, Long expireIn) {
        this.token = token;
        this.expireIn = expireIn;
    }
}
