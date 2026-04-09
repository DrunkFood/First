package com.jy.eletender.tenderdocument.dto.request;

import lombok.Data;

@Data
public class TenderDocumentScoreTypeSaveRequest {

    /**
     * 编制单级统一分值模式。
     * 全通过制评标办法允许为空。
     */
    private String scoreType;
}
