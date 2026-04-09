package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

@Data
public class TenderDocumentGenerateResponse {

    private Long tenderDocumentId;
    private Integer versionNo;
    private String status;
    private TenderDocumentFileView signedFile;
    private TenderDocumentFileView finalPackageFile;
    private TenderDocumentFileView compileInfoFile;
}
