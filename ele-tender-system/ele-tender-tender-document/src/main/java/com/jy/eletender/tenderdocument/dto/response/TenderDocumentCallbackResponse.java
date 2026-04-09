package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

@Data
public class TenderDocumentCallbackResponse {

    private String tenderId;
    private String fileRole;
    private Boolean success;
    private String responseCode;
    private String responseMessage;
    private Integer retryCount;
}
