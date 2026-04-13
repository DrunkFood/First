package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前电子标用户上下文
 */
@Data
public class IdentityContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appKey;

    private String externalUserId;

    private String externalUserName;

    private String enterpriseId;

    private String enterpriseName;

    private String enterpriseCode;

    public static IdentityContext from(ExternalUserInfoResponse response) {
        IdentityContext context = new IdentityContext();
        context.setAppKey(response.getAppKey());
        context.setExternalUserId(response.getUserId());
        context.setExternalUserName(response.getUserName());
        context.setEnterpriseId(response.getEnterpriseId());
        context.setEnterpriseName(response.getEnterpriseName());
        context.setEnterpriseCode(response.getEnterpriseCode());
        return context;
    }
}
