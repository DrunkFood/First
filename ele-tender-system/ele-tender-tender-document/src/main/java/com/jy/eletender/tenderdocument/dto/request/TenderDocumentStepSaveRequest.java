package com.jy.eletender.tenderdocument.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenderDocumentStepSaveRequest {

    @NotNull
    private Object payload;
}
