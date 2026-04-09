package com.jy.eletender.tenderdocument.dto.request;

import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenderDocumentCompleteStepRequest {

    /**
     * 固定步骤枚举，取值见 TenderDocumentStepCode。
     */
    @NotNull
    private TenderDocumentStepCode stepCode;
}
