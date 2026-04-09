package com.jy.eletender.tenderdocument.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEntryResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentOverviewResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentOverviewStepResponse;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.entity.TenderDocumentGenerationRecord;
import com.jy.eletender.tenderdocument.entity.TenderDocumentStep;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentGenerationStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepStatus;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentStepMapper;
import com.jy.eletender.tenderdocument.service.IProjectLockService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentGenerationRecordService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentService;
import com.jy.eletender.tenderdocument.support.TenderDocumentBasicInfoSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentCompileScopeResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentSyncGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 招标文件编制单主流程服务实现。
 * 职责：处理编制入口建单/命中、版本递增、重新编制、步骤壳数据查询与基础上下文落库。
 */
@Slf4j
@Service
public class TenderDocumentServiceImpl implements ITenderDocumentService {

    private final IProjectLockService projectLockService;
    private final TenderDocumentMapper tenderDocumentMapper;
    private final TenderDocumentStepMapper tenderDocumentStepMapper;
    private final TenderDocumentFileMapper tenderDocumentFileMapper;
    private final TenderDocumentSyncGateway tenderDocumentSyncGateway;
    private final ITenderDocumentGenerationRecordService tenderDocumentGenerationRecordService;
    private final long generationTimeoutSeconds;

    public TenderDocumentServiceImpl(IProjectLockService projectLockService,
                                     TenderDocumentMapper tenderDocumentMapper,
                                     TenderDocumentStepMapper tenderDocumentStepMapper,
                                     TenderDocumentFileMapper tenderDocumentFileMapper,
                                     TenderDocumentSyncGateway tenderDocumentSyncGateway,
                                     ITenderDocumentGenerationRecordService tenderDocumentGenerationRecordService,
                                     @Value("${tender-document.generation-timeout-seconds:1800}") long generationTimeoutSeconds) {
        this.projectLockService = projectLockService;
        this.tenderDocumentMapper = tenderDocumentMapper;
        this.tenderDocumentStepMapper = tenderDocumentStepMapper;
        this.tenderDocumentFileMapper = tenderDocumentFileMapper;
        this.tenderDocumentSyncGateway = tenderDocumentSyncGateway;
        this.tenderDocumentGenerationRecordService = tenderDocumentGenerationRecordService;
        this.generationTimeoutSeconds = generationTimeoutSeconds;
    }

    /**
     * 统一入口：取同维度最新版本编制单，按状态决定命中或创建。
     * DRAFT → 刷新同步结果并返回；COMPLETED → 原样返回（前端展示结果页）；
     * GENERATING 超时 → 重置为草稿后返回；GENERATING 未超时 → 原样返回。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentEntryResponse enter(TenderDocumentEntryRequest request, TenderDocumentUserContext userContext) {
        projectLockService.verifyOrCreateLock(request.getProjectId(), userContext);
        TenderDocumentBasicInfoSyncResult syncResult;
        try {
            syncResult = tenderDocumentSyncGateway.syncBasicInfo(request, userContext);
        } catch (Exception ex) {
            throw new BusinessException(TenderDocumentErrorCode.ENTRY_BASIC_INFO_SYNC_FAILED.getCode(),
                    TenderDocumentErrorCode.ENTRY_BASIC_INFO_SYNC_FAILED.getMessage() + ": " + ex.getMessage());
        }
        String compileScope = normalizeCompileScope(syncResult.getProjectType());

        TenderDocument latestDocument = findLatestDocument(request, compileScope);
        if (latestDocument != null) {
            if (TenderDocumentStatus.GENERATING.name().equals(latestDocument.getStatus())) {
                if (isGenerationTimedOut(latestDocument)) {
                    resetTimedOutGeneration(latestDocument);
                    fillDocumentFromSync(latestDocument, syncResult, request, compileScope);
                    tenderDocumentMapper.updateById(latestDocument);
                }
                return toEntryResponse(latestDocument);
            }
            if (TenderDocumentStatus.DRAFT.name().equals(latestDocument.getStatus())) {
                fillDocumentFromSync(latestDocument, syncResult, request, compileScope);
                tenderDocumentMapper.updateById(latestDocument);
            }
            return toEntryResponse(latestDocument);
        }

        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setBizType(request.getBizType());
        tenderDocument.setBizId(request.getBizId());
        fillDocumentFromSync(tenderDocument, syncResult, request, compileScope);
        tenderDocument.setStatus(TenderDocumentStatus.DRAFT.name());
        tenderDocument.setCurrentStepCode(TenderDocumentStepCode.BASIC_INFO.name());
        tenderDocument.setVersionNo(1);
        tenderDocumentMapper.insert(tenderDocument);

        initSteps(tenderDocument.getId());
        return toEntryResponse(tenderDocument);
    }

    /**
     * 返回页面壳数据，供前端渲染步骤条、当前状态和是否可编辑。
     */
    @Override
    public TenderDocumentOverviewResponse getOverview(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = tenderDocumentMapper.selectById(tenderDocumentId);
        if (tenderDocument == null) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getMessage());
        }

        projectLockService.verifyOrCreateLock(tenderDocument.getProjectId(), userContext);

        TenderDocumentOverviewResponse response = new TenderDocumentOverviewResponse();
        response.setTenderDocumentId(tenderDocument.getId());
        response.setStatus(tenderDocument.getStatus());
        response.setCurrentStepCode(tenderDocument.getCurrentStepCode());
        response.setVersionNo(tenderDocument.getVersionNo());
        response.setEditable(TenderDocumentStatus.DRAFT.name().equals(tenderDocument.getStatus()));
        response.setActive(Boolean.TRUE);

        List<TenderDocumentStep> stepList = tenderDocumentStepMapper.selectList(Wrappers.<TenderDocumentStep>lambdaQuery()
                .eq(TenderDocumentStep::getTenderDocumentId, tenderDocumentId)
                .orderByAsc(TenderDocumentStep::getStepOrder));
        for (TenderDocumentStep step : stepList) {
            TenderDocumentOverviewStepResponse stepView = new TenderDocumentOverviewStepResponse();
            stepView.setStepCode(step.getStepCode());
            stepView.setStepName(resolveStepName(step.getStepCode()));
            stepView.setStepOrder(step.getStepOrder());
            stepView.setStepStatus(step.getStepStatus());
            stepView.setActive(step.getStepCode().equals(tenderDocument.getCurrentStepCode()));
            response.getStepList().add(stepView);
        }
        return response;
    }

    /**
     * 创建编制单时初始化固定步骤，第一步默认进入进行中。
     */
    private void initSteps(Long tenderDocumentId) {
        for (TenderDocumentStepCode stepCode : TenderDocumentStepCode.orderedValues()) {
            TenderDocumentStep step = new TenderDocumentStep();
            step.setTenderDocumentId(tenderDocumentId);
            step.setStepCode(stepCode.name());
            step.setStepOrder(stepCode.getStepOrder());
            step.setStepStatus(stepCode == TenderDocumentStepCode.BASIC_INFO
                    ? TenderDocumentStepStatus.IN_PROGRESS.name()
                    : TenderDocumentStepStatus.PENDING.name());
            tenderDocumentStepMapper.insert(step);
        }
    }

    /**
     * 入口接口只返回后续页面联调用到的最小字段，避免把内部状态过度暴露给前端。
     */
    private TenderDocumentEntryResponse toEntryResponse(TenderDocument tenderDocument) {
        TenderDocumentEntryResponse response = new TenderDocumentEntryResponse();
        response.setTenderDocumentId(tenderDocument.getId());
        response.setProjectId(tenderDocument.getProjectId());
        response.setTenderId(tenderDocument.getTenderId());
        response.setStatus(tenderDocument.getStatus());
        response.setCurrentStepCode(tenderDocument.getCurrentStepCode());
        response.setVersionNo(tenderDocument.getVersionNo());
        return response;
    }

    /**
     * 重新编制：将已完成的编制单原地重置为草稿，版本号递增。
     * 步骤数据保留（仅 GENERATE_PACKAGE 回退），前端可直接重新触发生成。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentEntryResponse recompile(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = tenderDocumentMapper.selectById(tenderDocumentId);
        if (tenderDocument == null) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getMessage());
        }
        projectLockService.verifyOrCreateLock(tenderDocument.getProjectId(), userContext);
        if (!TenderDocumentStatus.COMPLETED.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.RECOMPILE_NOT_ALLOWED.getCode(),
                    TenderDocumentErrorCode.RECOMPILE_NOT_ALLOWED.getMessage());
        }

        tenderDocument.setVersionNo(tenderDocument.getVersionNo() + 1);
        tenderDocument.setStatus(TenderDocumentStatus.DRAFT.name());
        tenderDocumentMapper.updateById(tenderDocument);

        resetGeneratePackageStep(tenderDocumentId);
        deactivateFiles(tenderDocumentId, TenderDocumentFileType.FINAL_PACKAGE_FILE);
        deactivateFiles(tenderDocumentId, TenderDocumentFileType.COMPILE_INFO_PDF);

        return toEntryResponse(tenderDocument);
    }

    /**
     * 取同维度最新版本编制单，不区分状态，versionNo 最大的即为当前版本。
     */
    private TenderDocument findLatestDocument(TenderDocumentEntryRequest request, String compileScope) {
        return tenderDocumentMapper.selectOne(Wrappers.<TenderDocument>lambdaQuery()
                .eq(TenderDocument::getBizType, request.getBizType())
                .eq(TenderDocument::getBizId, request.getBizId())
                .eq(TenderDocument::getProjectId, request.getProjectId())
                .eq(TenderDocument::getCompileScope, compileScope)
                .eq(TenderDocumentScopeType.TENDER.name().equals(compileScope), TenderDocument::getTenderId, request.getTenderId())
                .orderByDesc(TenderDocument::getVersionNo)
                .last("limit 1"));
    }

    /**
     * compileScope 由电子标系统统一按 projectType 映射，避免业务系统直接控制编制粒度。
     */
    private String normalizeCompileScope(InteractionProjectType projectType) {
        try {
            return TenderDocumentCompileScopeResolver.resolve(projectType);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(TenderDocumentErrorCode.ENTRY_BASIC_INFO_SYNC_FAILED.getCode(), ex.getMessage());
        }
    }

    /**
     * 将同步回来的项目上下文覆盖到编制单主表，保证入口和页面展示使用同一份基础事实。
     */
    private void fillDocumentFromSync(TenderDocument tenderDocument,
                                      TenderDocumentBasicInfoSyncResult syncResult,
                                      TenderDocumentEntryRequest request,
                                      String compileScope) {
        tenderDocument.setCompileScope(compileScope);
        tenderDocument.setProjectId(request.getProjectId());
        // 项目级编制也保留入口 tenderId，便于后续问题排查和业务扩展时回溯来源上下文。
        tenderDocument.setTenderId(request.getTenderId());
        tenderDocument.setIndexOf(resolveTenderIndexOf(syncResult, request.getTenderId()));
        tenderDocument.setProjectCode(syncResult.getProjectCode());
        tenderDocument.setProjectName(syncResult.getProjectName());
        tenderDocument.setPurchaseMethod(syncResult.getPurchaseMethod());
        tenderDocument.setEvalMethod(syncResult.getEvalMethod() == null ? null : syncResult.getEvalMethod().name());
    }

    private Integer resolveTenderIndexOf(TenderDocumentBasicInfoSyncResult syncResult, String tenderId) {
        if (syncResult == null || tenderId == null || syncResult.getTenderList() == null) {
            return null;
        }
        for (Map<String, Object> tender : syncResult.getTenderList()) {
            if (Objects.equals(tenderId, tender.get("tenderId"))) {
                return toInteger(tender.get("indexOf"));
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isGenerationTimedOut(TenderDocument document) {
        if (document.getModifyTime() == null) {
            return true;
        }
        long elapsedSeconds = (System.currentTimeMillis() - document.getModifyTime().getTime()) / 1000;
        return elapsedSeconds > generationTimeoutSeconds;
    }

    /**
     * GENERATING 超时后回退为 DRAFT，并将残留的 PROCESSING 生成记录标记为失败。
     */
    private void resetTimedOutGeneration(TenderDocument document) {
        log.warn("TENDER_DOCUMENT ENTRY generation_timeout tenderDocumentId={} modifyTime={} timeoutSeconds={}",
                document.getId(), document.getModifyTime(), generationTimeoutSeconds);
        document.setStatus(TenderDocumentStatus.DRAFT.name());
        TenderDocumentGenerationRecord latestRecord =
                tenderDocumentGenerationRecordService.findLatestByTenderDocumentId(document.getId());
        if (latestRecord != null
                && TenderDocumentGenerationStatus.PROCESSING.name().equals(latestRecord.getGenerateStatus())) {
            tenderDocumentGenerationRecordService.markFail(
                    latestRecord.getId(), "GENERATION_TIMEOUT", "生成超时，自动重置", new Date());
        }
    }

    private void resetGeneratePackageStep(Long tenderDocumentId) {
        TenderDocumentStep generateStep = tenderDocumentStepMapper.selectOne(Wrappers.<TenderDocumentStep>lambdaQuery()
                .eq(TenderDocumentStep::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentStep::getStepCode, TenderDocumentStepCode.GENERATE_PACKAGE.name())
                .last("limit 1"));
        if (generateStep != null) {
            generateStep.setStepStatus(TenderDocumentStepStatus.IN_PROGRESS.name());
            generateStep.setCompleteTime(null);
            tenderDocumentStepMapper.updateById(generateStep);
        }
    }

    private void deactivateFiles(Long tenderDocumentId, TenderDocumentFileType fileRole) {
        List<TenderDocumentFile> files = tenderDocumentFileMapper.selectList(Wrappers.<TenderDocumentFile>lambdaQuery()
                .eq(TenderDocumentFile::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentFile::getFileRole, fileRole.name())
                .eq(TenderDocumentFile::getActiveFlag, 1));
        for (TenderDocumentFile file : files) {
            file.setActiveFlag(0);
            tenderDocumentFileMapper.updateById(file);
        }
    }

    /**
     * 概览接口优先复用枚举里的中文步骤名，避免前端自己维护映射。
     */
    private String resolveStepName(String stepCode) {
        for (TenderDocumentStepCode code : TenderDocumentStepCode.values()) {
            if (code.name().equals(stepCode)) {
                return code.getStepName();
            }
        }
        return stepCode;
    }
}
