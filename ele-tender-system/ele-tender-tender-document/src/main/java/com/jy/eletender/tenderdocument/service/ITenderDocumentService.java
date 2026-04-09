package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEntryResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentOverviewResponse;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;

public interface ITenderDocumentService {

    TenderDocumentEntryResponse enter(TenderDocumentEntryRequest request, TenderDocumentUserContext userContext);

    TenderDocumentOverviewResponse getOverview(Long tenderDocumentId, TenderDocumentUserContext userContext);

    TenderDocumentEntryResponse recompile(Long tenderDocumentId, TenderDocumentUserContext userContext);
}
