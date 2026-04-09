package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

@Data
public class TenderDocumentOverviewStepResponse {

    /**
     * 步骤枚举名，取值见 TenderDocumentStepCode。
     */
    private String stepCode;
    private String stepName;
    private Integer stepOrder;

    /**
     * 步骤状态枚举名，取值见 TenderDocumentStepStatus。
     */
    private String stepStatus;
    private Boolean active;
}
