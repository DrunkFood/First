package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentCallbackRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCallbackResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGeneratePageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerateResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerationRecordView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentUnifiedCallbackResponse;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;

import java.util.List;

public interface ITenderDocumentGenerationService {

    TenderDocumentGeneratePageResponse getGeneratePage(Long tenderDocumentId, TenderDocumentUserContext userContext);

    TenderDocumentGenerateResponse generate(Long tenderDocumentId, TenderDocumentUserContext userContext);

    TenderDocumentCallbackResponse callbackSignedFile(Long tenderDocumentId, TenderDocumentCallbackRequest request,
                                                      TenderDocumentUserContext userContext);

    TenderDocumentCallbackResponse callbackPackageFile(Long tenderDocumentId, TenderDocumentCallbackRequest request,
                                                       TenderDocumentUserContext userContext);

    TenderDocumentUnifiedCallbackResponse callbackAll(Long tenderDocumentId, TenderDocumentCallbackRequest request,
                                                      TenderDocumentUserContext userContext);

    List<TenderDocumentGenerationRecordView> listGenerateRecords(Long tenderDocumentId, TenderDocumentUserContext userContext);
}
