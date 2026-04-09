package com.jy.eletender.support.model.external;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 外部系统换取 Token 的内部结果对象。
 */
@Data
@AllArgsConstructor
public class ExternalTokenIssueResult {

    private String token;
    private Long expireIn;
}
