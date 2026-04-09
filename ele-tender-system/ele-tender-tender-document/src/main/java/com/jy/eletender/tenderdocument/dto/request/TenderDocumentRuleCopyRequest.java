package com.jy.eletender.tenderdocument.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenderDocumentRuleCopyRequest {

    @NotBlank
    private String sourceTenderId;

    @NotBlank
    private String targetTenderId;

    private String targetTenderName;
}
