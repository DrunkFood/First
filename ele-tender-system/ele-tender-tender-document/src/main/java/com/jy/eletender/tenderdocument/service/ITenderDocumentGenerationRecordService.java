package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentGenerationRecord;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;

import java.util.Date;
import java.util.List;

public interface ITenderDocumentGenerationRecordService {

    /**
     * 生成开始时创建 PROCESSING 记录。
     */
    TenderDocumentGenerationRecord createProcessingRecord(TenderDocument tenderDocument, TenderDocumentUserContext userContext);

    /**
     * 生成成功后更新记录为 SUCCESS。
     */
    void markSuccess(Long recordId, Date endTime);

    /**
     * 生成失败后更新记录为 FAIL，并保留错误码与错误信息。
     */
    void markFail(Long recordId, String errorCode, String errorMessage, Date endTime);

    TenderDocumentGenerationRecord findLatestByTenderDocumentId(Long tenderDocumentId);

    List<TenderDocumentGenerationRecord> listByTenderDocumentId(Long tenderDocumentId);
}
