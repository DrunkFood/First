package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 生成网关占位实现。
 * 在无外部文件服务接入时，使用内存序列号构造可落库的模拟文件信息。
 */
@Component
@ConditionalOnMissingBean(TenderDocumentGenerationGateway.class)
public class DefaultTenderDocumentGenerationGateway implements TenderDocumentGenerationGateway {

    private static final AtomicLong FILE_ID_SEQUENCE = new AtomicLong(100000L);
    private static final DateTimeFormatter FILE_NAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成占位数据包文件信息。
     */
    @Override
    public TenderDocumentGeneratedFile saveFinalPackage(TenderDocument tenderDocument,
                                                        TenderDocumentScopeType scopeType,
                                                        String tenderId,
                                                        byte[] encryptedContent,
                                                        String suffix) {
        TenderDocumentGeneratedFile file = new TenderDocumentGeneratedFile();
        file.setTenderId(tenderId);
        file.setScopeType(scopeType.name());
        file.setFileRole(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
        file.setFileId(FILE_ID_SEQUENCE.incrementAndGet());
        file.setFileName(buildPackageFileName(tenderDocument, scopeType, tenderId, suffix));
        file.setFileSize(encryptedContent == null ? 0L : (long) encryptedContent.length);
        file.setContentType("application/octet-stream");
        file.setFileSha256("sha256-" + file.getFileId());
        return file;
    }

    /**
     * 生成占位编制信息文件信息。
     */
    @Override
    public TenderDocumentGeneratedFile saveCompileInfoPdf(TenderDocument tenderDocument, byte[] pdfContent) {
        TenderDocumentGeneratedFile file = new TenderDocumentGeneratedFile();
        file.setScopeType(TenderDocumentScopeType.PROJECT.name());
        file.setFileRole(TenderDocumentFileType.COMPILE_INFO_PDF.name());
        file.setFileId(FILE_ID_SEQUENCE.incrementAndGet());
        file.setFileName(buildCompileInfoFileName(tenderDocument));
        file.setFileSize(pdfContent == null ? 0L : (long) pdfContent.length);
        file.setContentType("application/pdf");
        file.setFileSha256("sha256-" + file.getFileId());
        return file;
    }

    private String buildPackageFileName(TenderDocument tenderDocument, TenderDocumentScopeType scopeType, String tenderId, String suffix) {
        String bizCode = resolveBizCode(tenderDocument, scopeType, tenderId);
        return "[" + bizCode + "]采购文件电子数据包" + nowTimestamp() + suffix;
    }

    private String buildCompileInfoFileName(TenderDocument tenderDocument) {
        TenderDocumentScopeType scopeType = resolveScopeType(tenderDocument);
        String bizCode = resolveBizCode(tenderDocument, scopeType, tenderDocument.getTenderId());
        return "[" + bizCode + "]采购文件编制信息" + nowTimestamp() + ".pdf";
    }

    private TenderDocumentScopeType resolveScopeType(TenderDocument tenderDocument) {
        return StringUtils.hasText(tenderDocument.getCompileScope())
                && TenderDocumentScopeType.TENDER.name().equalsIgnoreCase(tenderDocument.getCompileScope())
                ? TenderDocumentScopeType.TENDER
                : TenderDocumentScopeType.PROJECT;
    }

    private String resolveBizCode(TenderDocument tenderDocument, TenderDocumentScopeType scopeType, String tenderId) {
        if (scopeType == TenderDocumentScopeType.TENDER) {
            if (!StringUtils.hasText(tenderId)) {
                throw new BusinessException(TenderDocumentErrorCode.TENDER_ID_REQUIRED.getCode(),
                        TenderDocumentErrorCode.TENDER_ID_REQUIRED.getMessage());
            }
            return tenderId;
        }
        String projectCode = StringUtils.hasText(tenderDocument.getProjectCode()) ? tenderDocument.getProjectCode() : tenderDocument.getProjectId();
        if (!StringUtils.hasText(projectCode)) {
            throw new BusinessException(TenderDocumentErrorCode.GENERATION_NOT_ALLOWED.getCode(), "缺少项目编号，不能生成文件名");
        }
        return projectCode;
    }

    private String nowTimestamp() {
        return LocalDateTime.now().format(FILE_NAME_TIME_FORMATTER);
    }
}
