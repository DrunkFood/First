package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleCopyRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentScoreTypeSaveRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEvaluationRulesPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentRuleResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentScoreTypeResponse;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;

public interface ITenderDocumentRuleService {

    TenderDocumentEvaluationRulesPageResponse getRulePage(Long tenderDocumentId, TenderDocumentUserContext userContext);

    void saveRuleTree(Long tenderDocumentId, String tenderId, TenderDocumentRuleSaveRequest request, TenderDocumentUserContext userContext);

    TenderDocumentRuleResponse getRuleTree(Long tenderDocumentId, String tenderId, String nodeCategory,
                                           TenderDocumentUserContext userContext);

    void copyRuleTree(Long tenderDocumentId, TenderDocumentRuleCopyRequest request, TenderDocumentUserContext userContext);

    TenderDocumentScoreTypeResponse getScoreType(Long tenderDocumentId, TenderDocumentUserContext userContext);

    void updateScoreType(Long tenderDocumentId, TenderDocumentScoreTypeSaveRequest request, TenderDocumentUserContext userContext);
}
