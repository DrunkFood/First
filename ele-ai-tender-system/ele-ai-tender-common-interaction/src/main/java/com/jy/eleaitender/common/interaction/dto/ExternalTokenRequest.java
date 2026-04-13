package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 外部系统 Token 请求
 */
@Data
public class ExternalTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userName;
    private String userId;
    private String enterpriseName;
    private String enterpriseId;
    private String enterpriseCode;
}
