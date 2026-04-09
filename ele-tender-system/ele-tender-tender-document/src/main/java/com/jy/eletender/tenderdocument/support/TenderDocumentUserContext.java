package com.jy.eletender.tenderdocument.support;

import lombok.Data;

/**
 * 当前外部用户上下文
 */
@Data
public class TenderDocumentUserContext {

    private String appKey;
    private String authorization;
    private String userId;
    private String userName;
    private String enterpriseId;
    private String enterpriseName;
    private String enterpriseCode;
}
