package com.jy.eletender.support.model.external;

import lombok.Data;

/**
 * 外部用户信息内部视图对象。
 */
@Data
public class ExternalUserInfoView {

    private String appKey;
    private String userId;
    private String userName;
    private String enterpriseId;
    private String enterpriseName;
    private String enterpriseCode;
}
