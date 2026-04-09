package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class TenderDocumentCallbackHistoryView {

    private String tenderId;
    private String fileRole;
    private String callbackStatus;
    private String responseCode;
    private String responseMessage;
    private Integer retryCount;
    private Date callbackTime;
}
