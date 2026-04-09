package com.jy.eletender.tenderdocument.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenderDocumentCallbackGatewayResult {

    /**
     * 交互层回传是否成功。
     */
    private boolean success;

    /**
     * 业务系统返回的响应码或业务码，直接记录到回传历史中。
     */
    private String responseCode;

    /**
     * 业务系统返回的响应消息，供页面展示和审计排障使用。
     */
    private String responseMessage;
}
