package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.tenderdocument.entity.TenderDocument;

/**
 * 生成产物网关。
 * 负责产出数据包与编制信息文件，并返回可落库的文件元数据。
 */
public interface TenderDocumentGenerationGateway {

    /**
     * 保存指定范围的最终数据包文件。
     */
    TenderDocumentGeneratedFile saveFinalPackage(TenderDocument tenderDocument,
                                                 TenderDocumentScopeType scopeType,
                                                 String tenderId,
                                                 byte[] encryptedContent,
                                                 String suffix);

    /**
     * 保存编制信息 PDF。
     */
    TenderDocumentGeneratedFile saveCompileInfoPdf(TenderDocument tenderDocument, byte[] pdfContent);
}
