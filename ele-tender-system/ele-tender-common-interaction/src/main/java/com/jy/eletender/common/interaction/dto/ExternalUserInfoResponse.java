package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 外部用户信息响应
 */
@Data
public class ExternalUserInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appKey;
    private String userId;
    private String userName;
    private String enterpriseId;
    private String enterpriseName;
    private String enterpriseCode;
}
