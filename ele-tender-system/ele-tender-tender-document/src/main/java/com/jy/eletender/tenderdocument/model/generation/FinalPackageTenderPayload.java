package com.jy.eletender.tenderdocument.model.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalPackageTenderPayload {

    private String tenderId;

    private String tenderName;

    private String tenderNo;

    private Map<String, Object> bidForms;

    private FinalPackageRulePayload bidEvalRules;
}
