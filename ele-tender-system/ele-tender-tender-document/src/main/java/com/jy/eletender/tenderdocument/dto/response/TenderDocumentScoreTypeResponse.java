package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

@Data
public class TenderDocumentScoreTypeResponse {

    /**
     * 编制单级统一分值模式。
     * 全通过制评标办法允许为空。
     */
    private String scoreType;
}
