package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TenderDocumentDetailResponse {

    private Long tenderDocumentId;
    private String projectId;
    private String projectCode;
    private String projectName;
    private String purchaseMethod;
    private String evalMethod;
    private String status;
    private String currentStepCode;
    private Integer versionNo;
    private List<TenderDocumentStepView> stepList = new ArrayList<>();
}
