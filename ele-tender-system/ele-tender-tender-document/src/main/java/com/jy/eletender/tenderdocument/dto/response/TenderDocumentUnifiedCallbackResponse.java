package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

@Data
public class TenderDocumentUnifiedCallbackResponse {

    private Boolean success;
    private String message;
    private TenderDocumentCallbackResponse signedFileResult;
    private TenderDocumentCallbackResponse packageFileResult;
}
