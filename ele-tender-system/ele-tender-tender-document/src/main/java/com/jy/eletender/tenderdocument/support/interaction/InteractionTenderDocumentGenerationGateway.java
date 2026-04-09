package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.support.TenderDocumentGeneratedFile;
import com.jy.eletender.tenderdocument.support.TenderDocumentGenerationGateway;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 对接文件服务的生成网关实现。
 * 负责汇总源文件、构建数据包与编制信息文件并上传到文件服务。
 */
@Primary
@Component
public class InteractionTenderDocumentGenerationGateway implements TenderDocumentGenerationGateway {

    private static final DateTimeFormatter FILE_NAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FileTransferClient fileTransferClient;
    private final TenderDocumentInteractionProperties properties;

    public InteractionTenderDocumentGenerationGateway(FileTransferClient fileTransferClient,
                                                      TenderDocumentInteractionProperties properties) {
        this.fileTransferClient = fileTransferClient;
        this.properties = properties;
    }

    /**
     * 保存最终采购文件数据包并上传文件服务。
     */
    @Override
    public TenderDocumentGeneratedFile saveFinalPackage(TenderDocument tenderDocument,
                                                        TenderDocumentScopeType scopeType,
                                                        String tenderId,
                                                        byte[] encryptedContent,
                                                        String suffix) {
        String fileName = buildPackageFileName(tenderDocument, scopeType, tenderId, suffix);
        // 数据包按二进制流上传，后续由业务系统按后缀识别为采购文件数据包。
        UploadedFileInfo upload = fileTransferClient.upload(fileName, properties.getUploadBizType(), "application/octet-stream", encryptedContent);
        return toGeneratedFile(scopeType.name(), TenderDocumentFileType.FINAL_PACKAGE_FILE.name(), tenderId, upload);
    }

    /**
     * 生成编制信息文件并上传文件服务。
     */
    @Override
    public TenderDocumentGeneratedFile saveCompileInfoPdf(TenderDocument tenderDocument, byte[] pdfContent) {
        String fileName = buildCompileInfoFileName(tenderDocument);
        UploadedFileInfo upload = fileTransferClient.upload(fileName, properties.getUploadBizType(), "application/pdf", pdfContent);
        // 采购文件编制信息固定按项目级归档，不参与业务系统回传。
        return toGeneratedFile(TenderDocumentScopeType.PROJECT.name(), TenderDocumentFileType.COMPILE_INFO_PDF.name(), null, upload);
    }

    private String buildPackageFileName(TenderDocument tenderDocument, TenderDocumentScopeType scopeType, String tenderId, String suffix) {
        String bizCode = resolveBizCode(tenderDocument, scopeType, tenderId);
        // 命名规则：[项目编号]采购文件电子数据包[yyyyMMddHHmmss].[suffix]
        return "[" + bizCode + "]采购文件电子数据包" + nowTimestamp() + suffix;
    }

    private String buildCompileInfoFileName(TenderDocument tenderDocument) {
        TenderDocumentScopeType scopeType = StringUtils.hasText(tenderDocument.getCompileScope())
                && TenderDocumentScopeType.TENDER.name().equalsIgnoreCase(tenderDocument.getCompileScope())
                ? TenderDocumentScopeType.TENDER
                : TenderDocumentScopeType.PROJECT;
        String bizCode = resolveBizCode(tenderDocument, scopeType, tenderDocument.getTenderId());
        return "[" + bizCode + "]采购文件编制信息" + nowTimestamp() + ".pdf";
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

    private TenderDocumentGeneratedFile toGeneratedFile(String scopeType,
                                                        String fileRole,
                                                        String tenderId,
                                                        UploadedFileInfo upload) {
        TenderDocumentGeneratedFile file = new TenderDocumentGeneratedFile();
        file.setScopeType(scopeType);
        file.setFileRole(fileRole);
        file.setTenderId(tenderId);
        file.setFileId(upload.getFileId());
        file.setFileName(upload.getFileName());
        file.setFileSize(upload.getFileSize());
        file.setContentType(fileRole.equals(TenderDocumentFileType.COMPILE_INFO_PDF.name()) ? "application/pdf" : "application/octet-stream");
        file.setFileSha256(upload.getFileSha256());
        return file;
    }
}
