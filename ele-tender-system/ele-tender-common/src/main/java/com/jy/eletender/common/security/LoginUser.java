package com.jy.eletender.common.security;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String realName;

    private String tokenType;

    private List<String> roles;

    private List<String> permissions;

    private String appKey;

    private String externalUserId;

    private String externalUserName;

    private String externalJti;

    private String enterpriseId;

    private String enterpriseName;

    private String enterpriseCode;
}
