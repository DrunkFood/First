package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

@Data
public class TenderDocumentEntryResponse {

    private Long tenderDocumentId;
    private String projectId;

    /**
     * 公开类项目返回时可能为空；邀请类项目返回当前标段ID。
     */
    private String tenderId;

    /**
     * 编制单状态枚举名，取值见 TenderDocumentStatus。
     */
    private String status;

    /**
     * 当前步骤枚举名，取值见 TenderDocumentStepCode。
     */
    private String currentStepCode;
    private Integer versionNo;
}
