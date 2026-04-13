package com.jy.eleaitender.support.model.external;

import lombok.Data;

/**
 * 外部系统换取 Token 的内部命令对象。
 */
@Data
public class ExternalTokenIssueCommand {

    private String userName;
    private String userId;
    private String enterpriseName;
    private String enterpriseId;
    private String enterpriseCode;
}
