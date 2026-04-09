package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;

/**
 * 生成产物回传网关。
 * 统一封装签章文件和数据包回传到业务系统的动作。
 */
public interface TenderDocumentCallbackGateway {

    /**
     * 回传签章文件。
     */
    TenderDocumentCallbackGatewayResult callbackSignedFile(TenderDocument tenderDocument, TenderDocumentFile file,
                                                           String tenderId, TenderDocumentUserContext userContext);

    /**
     * 回传数据包文件。
     */
    TenderDocumentCallbackGatewayResult callbackPackageFile(TenderDocument tenderDocument, TenderDocumentFile file,
                                                            String tenderId, TenderDocumentUserContext userContext);
}
