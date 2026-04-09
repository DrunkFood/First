package com.jy.eletender.tenderdocument.dto.request;

import lombok.Data;

@Data
public class TenderDocumentCallbackRequest {

    /**
     * 邀请类项目回传时必须传入标段ID；公开类项目可为空。
     */
    private String tenderId;
}
