package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TenderDocumentOverviewResponse {

    private Long tenderDocumentId;

    /**
     * 编制单状态枚举名，取值见 TenderDocumentStatus。
     */
    private String status;

    /**
     * 当前步骤枚举名，取值见 TenderDocumentStepCode。
     */
    private String currentStepCode;
    private Integer versionNo;
    private Boolean editable;

    /**
     * 当前概览对象自身是否处于激活态。
     * 页面通常固定为 true，步骤激活态在 stepList 中单独表达。
     */
    private Boolean active;
    private List<TenderDocumentOverviewStepResponse> stepList = new ArrayList<>();
}
