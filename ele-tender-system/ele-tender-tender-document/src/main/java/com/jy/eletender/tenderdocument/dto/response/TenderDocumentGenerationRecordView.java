package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class TenderDocumentGenerationRecordView {

    private Long id;
    private Integer versionNo;
    private String projectId;
    private String tenderId;
    private String generateStatus;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private String errorCode;
    private String errorMessage;
    private String traceId;
    private String operatorId;
    private String operatorName;
}
