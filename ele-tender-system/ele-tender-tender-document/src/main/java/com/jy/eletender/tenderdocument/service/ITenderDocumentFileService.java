package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentFileBindRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFilePageResponse;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;

public interface ITenderDocumentFileService {

    TenderDocumentFilePageResponse getPurchaseFilePage(Long tenderDocumentId, TenderDocumentUserContext userContext);

    void bindPurchaseFile(Long tenderDocumentId, TenderDocumentFileBindRequest request, TenderDocumentUserContext userContext);

    void removePurchaseFile(Long tenderDocumentId, String tenderId, TenderDocumentUserContext userContext);

    TenderDocumentFilePageResponse getSignedFilePage(Long tenderDocumentId, TenderDocumentUserContext userContext);

    void bindSignedFile(Long tenderDocumentId, TenderDocumentFileBindRequest request, TenderDocumentUserContext userContext);

    void removeSignedFile(Long tenderDocumentId, String tenderId, TenderDocumentUserContext userContext);
}
