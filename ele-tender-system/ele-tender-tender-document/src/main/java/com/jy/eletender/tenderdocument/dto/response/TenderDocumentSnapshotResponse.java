package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class TenderDocumentSnapshotResponse {

    private String stepCode;
    private String snapshotJson;
    private Date sourceSyncTime;
}
