package com.jy.eletender.tenderdocument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentFileBindRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFilePageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFileView;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.service.IProjectLockService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentFileService;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 招标文件文件绑定服务实现。
 * 职责：维护采购文件/签章文件与编制单的绑定关系，并按编制粒度控制同角色有效文件唯一性。
 */
@Service
public class TenderDocumentFileServiceImpl implements ITenderDocumentFileService {

    private static final DateTimeFormatter FILE_NAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TenderDocumentMapper tenderDocumentMapper;
    private final TenderDocumentFileMapper tenderDocumentFileMapper;
    private final IProjectLockService projectLockService;

    public TenderDocumentFileServiceImpl(TenderDocumentMapper tenderDocumentMapper,
                                         TenderDocumentFileMapper tenderDocumentFileMapper,
                                         IProjectLockService projectLockService) {
        this.tenderDocumentMapper = tenderDocumentMapper;
        this.tenderDocumentFileMapper = tenderDocumentFileMapper;
        this.projectLockService = projectLockService;
    }

    /**
     * 返回采购文件页当前已绑定的有效文件列表。
     */
    @Override
    public TenderDocumentFilePageResponse getPurchaseFilePage(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        return buildFilePage(tenderDocumentId, TenderDocumentFileType.PURCHASE_SOURCE_PDF, userContext);
    }

    /**
     * 绑定采购文件。文件二进制仍由文件服务承载，这里只保存业务绑定关系。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPurchaseFile(Long tenderDocumentId, TenderDocumentFileBindRequest request, TenderDocumentUserContext userContext) {
        bindFile(tenderDocumentId, request, TenderDocumentFileType.PURCHASE_SOURCE_PDF, userContext);
    }

    /**
     * 删除采购文件本质是取消当前编制单与文件的绑定，不会删除文件服务中的原始文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePurchaseFile(Long tenderDocumentId, String tenderId, TenderDocumentUserContext userContext) {
        deactivateFile(tenderDocumentId, tenderId, TenderDocumentFileType.PURCHASE_SOURCE_PDF, userContext);
    }

    /**
     * 返回采购文件环节当前已绑定的有效签章文件。
     */
    @Override
    public TenderDocumentFilePageResponse getSignedFilePage(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        return buildFilePage(tenderDocumentId, TenderDocumentFileType.SIGNED_PDF, userContext);
    }

    /**
     * 绑定签章文件，覆盖当前同粒度下的旧签章文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindSignedFile(Long tenderDocumentId, TenderDocumentFileBindRequest request, TenderDocumentUserContext userContext) {
        bindFile(tenderDocumentId, request, TenderDocumentFileType.SIGNED_PDF, userContext);
    }

    /**
     * 取消签章文件绑定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSignedFile(Long tenderDocumentId, String tenderId, TenderDocumentUserContext userContext) {
        deactivateFile(tenderDocumentId, tenderId, TenderDocumentFileType.SIGNED_PDF, userContext);
    }

    /**
     * 同一编制单同一粒度同一文件角色只保留一条有效记录；重新绑定时先让旧记录失效。
     */
    private void bindFile(Long tenderDocumentId, TenderDocumentFileBindRequest request, TenderDocumentFileType fileRole,
                          TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireEditableDocument(tenderDocumentId, userContext);
        TenderDocumentScopeType scopeType = resolveScopeType(tenderDocument);
        String resolvedTenderId = resolveTenderId(scopeType, request.getTenderId());

        TenderDocumentFile existingFile = selectActiveFile(tenderDocumentId, resolvedTenderId, scopeType, fileRole);
        if (existingFile != null) {
            existingFile.setActiveFlag(0);
            tenderDocumentFileMapper.updateById(existingFile);
        }

        TenderDocumentFile file = new TenderDocumentFile();
        file.setTenderDocumentId(tenderDocumentId);
        file.setProjectId(tenderDocument.getProjectId());
        file.setTenderId(resolvedTenderId);
        file.setScopeType(scopeType.name());
        file.setFileRole(fileRole.name());
        file.setFileId(request.getFileId());
        file.setFileName(TenderDocumentFileType.SIGNED_PDF == fileRole
                ? appendUploadTimestampToPdfName(request.getFileName())
                : request.getFileName());
        file.setFileSize(request.getFileSize());
        file.setContentType(request.getContentType());
        file.setFileSha256(request.getFileSha256());
        file.setActiveFlag(1);
        file.setCreatedTime(new Date());
        tenderDocumentFileMapper.insert(file);
    }

    private String appendUploadTimestampToPdfName(String fileName) {
        String normalized = StringUtils.hasText(fileName) ? fileName.trim() : "签章文件.pdf";
        String lowerCase = normalized.toLowerCase();
        String baseName = lowerCase.endsWith(".pdf")
                ? normalized.substring(0, normalized.length() - 4)
                : normalized;
        return baseName + LocalDateTime.now().format(FILE_NAME_TIME_FORMATTER) + ".pdf";
    }

    /**
     * 文件删除采用逻辑失效，便于后续审计“曾经绑定过哪些文件”。
     */
    private void deactivateFile(Long tenderDocumentId, String tenderId, TenderDocumentFileType fileRole,
                                TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireEditableDocument(tenderDocumentId, userContext);
        TenderDocumentScopeType scopeType = resolveScopeType(tenderDocument);
        String resolvedTenderId = resolveTenderId(scopeType, tenderId);
        TenderDocumentFile existingFile = selectActiveFile(tenderDocumentId, resolvedTenderId, scopeType, fileRole);
        if (existingFile != null) {
            existingFile.setActiveFlag(0);
            tenderDocumentFileMapper.updateById(existingFile);
        }
    }

    /**
     * 文件操作必须建立在“编制单存在 + 项目锁通过 + 当前仍可编辑”三个条件之上。
     */
    private TenderDocument requireEditableDocument(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = tenderDocumentMapper.selectById(tenderDocumentId);
        if (tenderDocument == null) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getMessage());
        }
        ProjectLock ignored = projectLockService.verifyOrCreateLock(tenderDocument.getProjectId(), userContext);
        if (!TenderDocumentStatus.DRAFT.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_EDITABLE.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_EDITABLE.getMessage());
        }
        return tenderDocument;
    }

    /**
     * 编制粒度直接决定文件是按项目绑定还是按标段绑定。
     */
    private TenderDocumentScopeType resolveScopeType(TenderDocument tenderDocument) {
        return StringUtils.hasText(tenderDocument.getCompileScope()) && TenderDocumentScopeType.TENDER.name().equalsIgnoreCase(tenderDocument.getCompileScope())
                ? TenderDocumentScopeType.TENDER
                : TenderDocumentScopeType.PROJECT;
    }

    /**
     * 邀请类项目要求文件绑定时显式传入标段ID，公开类项目则始终为空。
     */
    private String resolveTenderId(TenderDocumentScopeType scopeType, String tenderId) {
        if (scopeType == TenderDocumentScopeType.TENDER) {
            if (!StringUtils.hasText(tenderId)) {
                throw new BusinessException(TenderDocumentErrorCode.TENDER_ID_REQUIRED.getCode(),
                        "邀请类项目绑定文件时必须传入标段ID");
            }
            return tenderId;
        }
        return null;
    }

    /**
     * 统一查询当前仍有效的文件绑定记录。
     */
    private TenderDocumentFile selectActiveFile(Long tenderDocumentId, String tenderId, TenderDocumentScopeType scopeType,
                                                TenderDocumentFileType fileRole) {
        LambdaQueryWrapper<TenderDocumentFile> queryWrapper = new LambdaQueryWrapper<TenderDocumentFile>()
                .eq(TenderDocumentFile::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentFile::getScopeType, scopeType.name())
                .eq(TenderDocumentFile::getFileRole, fileRole.name())
                .eq(TenderDocumentFile::getActiveFlag, 1)
                .last("limit 1");
        if (scopeType == TenderDocumentScopeType.TENDER) {
            queryWrapper.eq(TenderDocumentFile::getTenderId, tenderId);
        } else {
            queryWrapper.isNull(TenderDocumentFile::getTenderId);
        }
        return tenderDocumentFileMapper.selectOne(queryWrapper);
    }

    /**
     * 页面返回结构始终是列表，公开类和邀请类都走同一套前端渲染模型。
     */
    private TenderDocumentFilePageResponse buildFilePage(Long tenderDocumentId, TenderDocumentFileType fileRole,
                                                         TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = tenderDocumentMapper.selectById(tenderDocumentId);
        if (tenderDocument == null) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getMessage());
        }
        ProjectLock ignored = projectLockService.verifyOrCreateLock(tenderDocument.getProjectId(), userContext);
        TenderDocumentFilePageResponse response = new TenderDocumentFilePageResponse();
        List<TenderDocumentFile> files = tenderDocumentFileMapper.selectList(new LambdaQueryWrapper<TenderDocumentFile>()
                .eq(TenderDocumentFile::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentFile::getFileRole, fileRole.name())
                .eq(TenderDocumentFile::getActiveFlag, 1)
                .orderByAsc(TenderDocumentFile::getTenderId, TenderDocumentFile::getId));
        for (TenderDocumentFile file : files) {
            // 这里仅返回业务可读信息，tenderName 暂按 tenderId 兜底，避免页面空值。
            TenderDocumentFileView view = new TenderDocumentFileView();
            view.setTenderId(file.getTenderId());
            view.setTenderName(StringUtils.hasText(file.getTenderId()) ? file.getTenderId() : null);
            view.setScopeType(file.getScopeType());
            view.setFileRole(file.getFileRole());
            view.setFileId(file.getFileId());
            view.setFileName(file.getFileName());
            view.setFileSize(file.getFileSize());
            view.setContentType(file.getContentType());
            view.setFileSha256(file.getFileSha256());
            view.setCreateTime(file.getCreatedTime());
            response.getFileList().add(view);
        }
        return response;
    }
}
