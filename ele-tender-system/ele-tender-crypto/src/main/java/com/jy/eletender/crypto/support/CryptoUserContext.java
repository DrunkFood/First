package com.jy.eletender.crypto.support;

import lombok.Data;

@Data
public class CryptoUserContext {

    private String appKey;

    private String authorization;

    private String userId;

    private String userName;

    private String enterpriseId;

    private String enterpriseName;

    private String enterpriseCode;
}
