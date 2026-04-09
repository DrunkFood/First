package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

@Data
public class TenderDocumentStepView {

    private String stepCode;
    private Integer stepOrder;
    private String stepStatus;
}
