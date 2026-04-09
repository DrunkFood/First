package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;

/**
 * 招标文件编制对外同步网关。
 * 封装与外部业务系统的数据同步能力，支持占位实现与正式交互实现切换。
 */
public interface TenderDocumentSyncGateway {

    /**
     * 编制入口场景同步基本信息。
     */
    TenderDocumentBasicInfoSyncResult syncBasicInfo(TenderDocumentEntryRequest request, TenderDocumentUserContext userContext);

    /**
     * 已有编制单场景同步基本信息。
     */
    TenderDocumentBasicInfoSyncResult syncBasicInfo(TenderDocument tenderDocument, TenderDocumentUserContext userContext);

    /**
     * 同步开标标录方案数据。
     */
    TenderDocumentBidRecordSyncResult syncBidRecord(TenderDocument tenderDocument, TenderDocumentUserContext userContext);
}
