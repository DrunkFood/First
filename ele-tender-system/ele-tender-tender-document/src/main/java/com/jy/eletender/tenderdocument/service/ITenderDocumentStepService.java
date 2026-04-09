package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.tenderdocument.dto.response.TenderDocumentBasicInfoPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentBidRecordPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCheckItemsResponse;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;

public interface ITenderDocumentStepService {

    TenderDocumentBasicInfoPageResponse getBasicInfoPage(Long tenderDocumentId, TenderDocumentUserContext userContext);

    TenderDocumentBasicInfoPageResponse syncBasicInfo(Long tenderDocumentId, TenderDocumentUserContext userContext);

    TenderDocumentBidRecordPageResponse getBidRecordPage(Long tenderDocumentId, TenderDocumentUserContext userContext);

    TenderDocumentBidRecordPageResponse syncBidRecord(Long tenderDocumentId, TenderDocumentUserContext userContext);

    void completeStep(Long tenderDocumentId, TenderDocumentStepCode stepCode, TenderDocumentUserContext userContext);

    TenderDocumentCheckItemsResponse getCheckItems(Long tenderDocumentId, TenderDocumentUserContext userContext);
}
