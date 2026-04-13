package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 身份查询响应
 */
@Data
public class IdentityQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String userName;

    private String realName;

    private String phone;

    private String email;

    private String enterpriseId;

    private String enterpriseName;

    private String enterpriseCode;
}
