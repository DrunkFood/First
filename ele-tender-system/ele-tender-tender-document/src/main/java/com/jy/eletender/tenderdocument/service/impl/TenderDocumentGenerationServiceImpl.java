package com.jy.eletender.tenderdocument.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.CaKeysInfoResponse;
import com.jy.eletender.common.logging.TraceContext;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentCallbackRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCallbackHistoryView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCallbackResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFileView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGeneratePageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerateResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerationRecordView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentUnifiedCallbackResponse;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocumentCaKeysSnapshot;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentCallback;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.entity.TenderDocumentGenerationRecord;
import com.jy.eletender.tenderdocument.entity.TenderDocumentStep;
import com.jy.eletender.tenderdocument.entity.TenderDocumentVersion;
import com.jy.eletender.tenderdocument.entity.TenderRuleHeader;
import com.jy.eletender.tenderdocument.enums.TenderDocumentCallbackStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentGenerationStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepStatus;
import com.jy.eletender.common.constant.FileConstants;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.tenderdocument.mapper.ExternalSystemAccessMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentCallbackCounterMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentCallbackMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentCaKeysSnapshotMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentStepMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentVersionMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.model.generation.FinalPackagePayload;
import com.jy.eletender.tenderdocument.service.IProjectLockService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentGenerationRecordService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentGenerationService;
import com.jy.eletender.tenderdocument.support.TenderDocumentCallbackGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentCallbackGatewayResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentGeneratedFile;
import com.jy.eletender.tenderdocument.support.TenderDocumentGenerationGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import com.jy.eletender.tenderdocument.support.generation.CompileInfoPdfGenerator;
import com.jy.eletender.tenderdocument.support.generation.FinalPackageDataAssembler;
import com.jy.eletender.tenderdocument.support.generation.FinalPackageEncryptor;
import com.jy.eletender.tenderdocument.support.generation.SignedPdfContentLoader;
import com.jy.eletender.tenderdocument.support.generation.TenderDocumentUniqueCodeGenerator;
import com.jy.eletender.tenderdocument.support.generation.TenderDocumentVersionProperties;
import com.jy.eletender.tenderdocument.support.interaction.BusinessSystemRemoteClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 招标文件生成与回传服务实现。
 * 职责：负责生成产物固化、版本留存、回传调用及回传历史审计。
 */
@Slf4j
@Service
public class TenderDocumentGenerationServiceImpl implements ITenderDocumentGenerationService {

    private static final String CALLBACK_LOCK_KEY_PREFIX = "tender-document:callback:lock:";
    private static final long CALLBACK_LOCK_EXPIRE_SECONDS = 60L;
    private static final String EMPTY_TENDER_ID = "";
    private static final DefaultRedisScript<Long> RELEASE_CALLBACK_LOCK_SCRIPT = buildReleaseLockScript();

    private final TenderDocumentMapper tenderDocumentMapper;
    private final TenderDocumentStepMapper tenderDocumentStepMapper;
    private final TenderDocumentFileMapper tenderDocumentFileMapper;
    private final TenderDocumentCaKeysSnapshotMapper tenderDocumentCaKeysSnapshotMapper;
    private final TenderDocumentVersionMapper tenderDocumentVersionMapper;
    private final TenderDocumentCallbackMapper tenderDocumentCallbackMapper;
    private final TenderDocumentCallbackCounterMapper tenderDocumentCallbackCounterMapper;
    private final TenderRuleHeaderMapper tenderRuleHeaderMapper;
    private final IProjectLockService projectLockService;
    private final TenderDocumentGenerationGateway tenderDocumentGenerationGateway;
    private final TenderDocumentCallbackGateway tenderDocumentCallbackGateway;
    private final CompileInfoPdfGenerator compileInfoPdfGenerator;
    private final FinalPackageDataAssembler finalPackageDataAssembler;
    private final SignedPdfContentLoader signedPdfContentLoader;
    private final FinalPackageEncryptor finalPackageEncryptor;
    private final TenderDocumentUniqueCodeGenerator tenderDocumentUniqueCodeGenerator;
    private final TenderDocumentVersionProperties tenderDocumentVersionProperties;
    private final ITenderDocumentGenerationRecordService tenderDocumentGenerationRecordService;
    private final BusinessSystemRemoteClient businessSystemRemoteClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final ExternalSystemAccessMapper externalSystemAccessMapper;

    private static DefaultRedisScript<Long> buildReleaseLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<Long>();
        script.setResultType(Long.class);
        script.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        return script;
    }

    public TenderDocumentGenerationServiceImpl(TenderDocumentMapper tenderDocumentMapper,
                                               TenderDocumentStepMapper tenderDocumentStepMapper,
                                               TenderDocumentFileMapper tenderDocumentFileMapper,
                                               TenderDocumentCaKeysSnapshotMapper tenderDocumentCaKeysSnapshotMapper,
                                               TenderDocumentVersionMapper tenderDocumentVersionMapper,
                                               TenderDocumentCallbackMapper tenderDocumentCallbackMapper,
                                               TenderDocumentCallbackCounterMapper tenderDocumentCallbackCounterMapper,
                                               TenderRuleHeaderMapper tenderRuleHeaderMapper,
                                               IProjectLockService projectLockService,
                                               TenderDocumentGenerationGateway tenderDocumentGenerationGateway,
                                               TenderDocumentCallbackGateway tenderDocumentCallbackGateway,
                                               CompileInfoPdfGenerator compileInfoPdfGenerator,
                                               FinalPackageDataAssembler finalPackageDataAssembler,
                                               SignedPdfContentLoader signedPdfContentLoader,
                                               FinalPackageEncryptor finalPackageEncryptor,
                                               TenderDocumentUniqueCodeGenerator tenderDocumentUniqueCodeGenerator,
                                               TenderDocumentVersionProperties tenderDocumentVersionProperties,
                                               ITenderDocumentGenerationRecordService tenderDocumentGenerationRecordService,
                                               BusinessSystemRemoteClient businessSystemRemoteClient,
                                               ObjectMapper objectMapper,
                                               StringRedisTemplate stringRedisTemplate,
                                               PlatformTransactionManager transactionManager,
                                               ExternalSystemAccessMapper externalSystemAccessMapper) {
        this.tenderDocumentMapper = tenderDocumentMapper;
        this.tenderDocumentStepMapper = tenderDocumentStepMapper;
        this.tenderDocumentFileMapper = tenderDocumentFileMapper;
        this.tenderDocumentCaKeysSnapshotMapper = tenderDocumentCaKeysSnapshotMapper;
        this.tenderDocumentVersionMapper = tenderDocumentVersionMapper;
        this.tenderDocumentCallbackMapper = tenderDocumentCallbackMapper;
        this.tenderDocumentCallbackCounterMapper = tenderDocumentCallbackCounterMapper;
        this.tenderRuleHeaderMapper = tenderRuleHeaderMapper;
        this.projectLockService = projectLockService;
        this.tenderDocumentGenerationGateway = tenderDocumentGenerationGateway;
        this.tenderDocumentCallbackGateway = tenderDocumentCallbackGateway;
        this.compileInfoPdfGenerator = compileInfoPdfGenerator;
        this.finalPackageDataAssembler = finalPackageDataAssembler;
        this.signedPdfContentLoader = signedPdfContentLoader;
        this.finalPackageEncryptor = finalPackageEncryptor;
        this.tenderDocumentUniqueCodeGenerator = tenderDocumentUniqueCodeGenerator;
        this.tenderDocumentVersionProperties = tenderDocumentVersionProperties;
        this.tenderDocumentGenerationRecordService = tenderDocumentGenerationRecordService;
        this.businessSystemRemoteClient = businessSystemRemoteClient;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.externalSystemAccessMapper = externalSystemAccessMapper;
    }

    /**
     * 生成页聚合展示当前版本的签章文件、数据包、编制信息 PDF 和回传历史。
     */
    @Override
    public TenderDocumentGeneratePageResponse getGeneratePage(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        TenderDocumentGeneratePageResponse response = new TenderDocumentGeneratePageResponse();
        response.setStatus(tenderDocument.getStatus());
        response.setVersionNo(tenderDocument.getVersionNo());

        TenderDocumentFileView signedFileView = null;
        TenderDocumentFileView finalPackageView = null;
        TenderDocumentFileView compileInfoView = null;

        List<TenderDocumentFile> files = tenderDocumentFileMapper.selectList(Wrappers.<TenderDocumentFile>lambdaQuery()
                .eq(TenderDocumentFile::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentFile::getActiveFlag, 1)
                .orderByAsc(TenderDocumentFile::getFileRole, TenderDocumentFile::getTenderId, TenderDocumentFile::getId));
        for (TenderDocumentFile file : files) {
            TenderDocumentFileView view = toView(file);
            String role = file.getFileRole();
            if (TenderDocumentFileType.SIGNED_PDF.name().equals(role)) {
                if (signedFileView != null) {
                    throw new BusinessException(TenderDocumentErrorCode.GENERATED_FILE_MISSING.getCode(), "存在多个签章文件，请先清理旧记录");
                }
                signedFileView = view;
            } else if (TenderDocumentFileType.FINAL_PACKAGE_FILE.name().equals(role)) {
                if (finalPackageView != null) {
                    throw new BusinessException(TenderDocumentErrorCode.GENERATED_FILE_MISSING.getCode(), "存在多份数据包，请先清理旧记录");
                }
                finalPackageView = view;
                finalPackageView.setFileRole(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
            } else if (TenderDocumentFileType.COMPILE_INFO_PDF.name().equals(role)) {
                if (compileInfoView != null) {
                    throw new BusinessException(TenderDocumentErrorCode.GENERATED_FILE_MISSING.getCode(), "存在多个编制信息，请先清理旧记录");
                }
                compileInfoView = view;
            }
        }
        response.setSignedFile(signedFileView);
        response.setFinalPackageFile(finalPackageView);
        response.setCompileInfoFile(compileInfoView);
        TenderDocumentVersion currentVersion = tenderDocumentVersionMapper.selectOne(Wrappers.<TenderDocumentVersion>lambdaQuery()
                .eq(TenderDocumentVersion::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentVersion::getVersionNo, tenderDocument.getVersionNo())
                .orderByDesc(TenderDocumentVersion::getId)
                .last("limit 1"));
        response.setCompileCompleteTime(currentVersion != null
                && currentVersion.getVersionNo() != null
                && currentVersion.getVersionNo().equals(tenderDocument.getVersionNo())
                ? currentVersion.getGenerateTime()
                : null);
        TenderDocumentGenerationRecord latestRecord = tenderDocumentGenerationRecordService.findLatestByTenderDocumentId(tenderDocumentId);
        response.setLatestGenerateRecord(latestRecord != null
                && latestRecord.getVersionNo() != null
                && latestRecord.getVersionNo().equals(tenderDocument.getVersionNo())
                ? toView(latestRecord)
                : null);

        List<TenderDocumentCallback> callbacks = tenderDocumentCallbackMapper.selectList(Wrappers.<TenderDocumentCallback>lambdaQuery()
                .eq(TenderDocumentCallback::getTenderDocumentId, tenderDocumentId)
                .orderByDesc(TenderDocumentCallback::getCallbackTime, TenderDocumentCallback::getId));
        for (TenderDocumentCallback callback : callbacks) {
            TenderDocumentCallbackHistoryView view = new TenderDocumentCallbackHistoryView();
            view.setTenderId(callback.getTenderId());
            view.setFileRole(callback.getFileRole());
            view.setCallbackStatus(callback.getCallbackStatus());
            view.setResponseCode(callback.getResponseCode());
            view.setResponseMessage(callback.getResponseMessage());
            view.setRetryCount(callback.getRetryCount());
            view.setCallbackTime(callback.getCallbackTime());
            response.getCallbackHistory().add(view);
        }
        return response;
    }

    @Override
    public List<TenderDocumentGenerationRecordView> listGenerateRecords(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        requireDocument(tenderDocumentId, userContext);
        List<TenderDocumentGenerationRecord> records = tenderDocumentGenerationRecordService.listByTenderDocumentId(tenderDocumentId);
        List<TenderDocumentGenerationRecordView> views = new ArrayList<>();
        for (TenderDocumentGenerationRecord record : records) {
            views.add(toView(record));
        }
        return views;
    }

    /**
     * 生成采购文件数据包并固化版本。每份编制单只生成一份最终数据包。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentGenerateResponse generate(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        // 先做状态预检，再通过原子更新抢占“生成中”状态，避免并发重复生成。
        assertGenerationAllowed(tenderDocument);
        claimGeneratingStatus(tenderDocumentId);
        Long generationRecordId = null;
        Date generationStartTime = new Date();

        TenderDocumentFileView signedFileView = null;
        try {
            // 先落一条 PROCESSING 记录，后续统一在成功/失败分支更新同一条记录。
            TenderDocumentGenerationRecord generationRecord = tenderDocumentGenerationRecordService.createProcessingRecord(tenderDocument, userContext);
            generationRecordId = generationRecord == null ? null : generationRecord.getId();
            log.info("TENDER_DOCUMENT GENERATE phase=start status={} tenderDocumentId={} projectId={} tenderId={} versionNo={} recordId={}",
                    TenderDocumentGenerationStatus.PROCESSING.name(),
                    tenderDocumentId,
                    tenderDocument.getProjectId(),
                    tenderDocument.getTenderId(),
                    tenderDocument.getVersionNo(),
                    generationRecordId);
            // 校验前置步骤全部完成后，才允许进入最终产物生成。
            validateGenerationPreconditions(tenderDocumentId);
            TenderDocumentScopeType scopeType = resolveScopeType(tenderDocument);
            String tenderId = resolveTenderId(scopeType, tenderDocument.getTenderId());
            requireActiveFile(tenderDocumentId, TenderDocumentFileType.PURCHASE_SOURCE_PDF, scopeType, tenderId);
            TenderDocumentFile signedPdfFile = requireActiveFile(tenderDocumentId, TenderDocumentFileType.SIGNED_PDF, scopeType, tenderId);
            signedFileView = toView(signedPdfFile);

            // 组装 FINAL_PACKAGE_FILE 明文 JSON，并嵌入签章后的 PDF Base64。
            FinalPackagePayload finalPackagePayload = buildFinalPackagePayload(tenderDocument, signedPdfFile, userContext);
            // 对明文 JSON 做 AES-GCM 加密，得到最终上传的数据包二进制。
            byte[] encryptedPackageBytes = encryptFinalPackage(finalPackagePayload);
            String tenderDocumentSuffix = resolveTenderDocumentSuffix(userContext);
            TenderDocumentGeneratedFile finalPackageFile = tenderDocumentGenerationGateway.saveFinalPackage(
                    tenderDocument,
                    scopeType,
                    tenderId,
                    encryptedPackageBytes,
                    tenderDocumentSuffix
            );
            log.info("TENDER_DOCUMENT GENERATE phase=save_final_package success=true tenderDocumentId={} recordId={} fileId={} fileName={}",
                    tenderDocumentId, generationRecordId, finalPackageFile.getFileId(), finalPackageFile.getFileName());
            Date completedAt = new Date();
            // 采购文件编制信息 PDF 使用”打包完成时间”作为编制完成时间。
            java.util.Map<String, Object> basicInfoProjectInfo = finalPackageDataAssembler.readBasicInfoProjectInfo(tenderDocumentId);
            byte[] compileInfoPdfBytes = compileInfoPdfGenerator.generate(tenderDocument, basicInfoProjectInfo, finalPackageFile, userContext, completedAt);
            TenderDocumentGeneratedFile receiptFile = tenderDocumentGenerationGateway.saveCompileInfoPdf(tenderDocument, compileInfoPdfBytes);
            log.info("TENDER_DOCUMENT GENERATE phase=save_compile_info success=true tenderDocumentId={} recordId={} fileId={} fileName={}",
                    tenderDocumentId, generationRecordId, receiptFile.getFileId(), receiptFile.getFileName());

            // 新产物落库前先失效同角色旧记录，确保“当前有效文件”只有一份。
            deactivateGeneratedFiles(tenderDocumentId, TenderDocumentFileType.FINAL_PACKAGE_FILE);
            deactivateGeneratedFiles(tenderDocumentId, TenderDocumentFileType.COMPILE_INFO_PDF);
            tenderDocumentFileMapper.insert(toEntity(tenderDocument, finalPackageFile));
            tenderDocumentFileMapper.insert(toEntity(tenderDocument, receiptFile));

            TenderDocumentVersion version = new TenderDocumentVersion();
            version.setTenderDocumentId(tenderDocumentId);
            version.setVersionNo(tenderDocument.getVersionNo());
            version.setProjectId(tenderDocument.getProjectId());
            version.setStatus(TenderDocumentStatus.COMPLETED.name());
            version.setGenerateTime(completedAt);
            version.setSummaryJson(writeSummaryJson(List.of(finalPackageFile)));
            tenderDocumentVersionMapper.insert(version);
            saveCaKeysSnapshot(tenderDocument, userContext, completedAt, finalPackagePayload.getCaKeysInfo());

            TenderDocumentStep generateStep = tenderDocumentStepMapper.selectOne(Wrappers.<TenderDocumentStep>lambdaQuery()
                    .eq(TenderDocumentStep::getTenderDocumentId, tenderDocumentId)
                    .eq(TenderDocumentStep::getStepCode, TenderDocumentStepCode.GENERATE_PACKAGE.name())
                    .last("limit 1"));
            if (generateStep != null) {
                generateStep.setStepStatus(TenderDocumentStepStatus.COMPLETED.name());
                generateStep.setCompleteTime(completedAt);
                tenderDocumentStepMapper.updateById(generateStep);
            }

            // 生成成功后锁定编制单状态，后续不允许再次编辑或重新生成。
            tenderDocument.setStatus(TenderDocumentStatus.COMPLETED.name());
            tenderDocument.setCurrentStepCode(TenderDocumentStepCode.GENERATE_PACKAGE.name());
            tenderDocumentMapper.updateById(tenderDocument);

            TenderDocumentGenerateResponse response = new TenderDocumentGenerateResponse();
            response.setTenderDocumentId(tenderDocumentId);
            response.setVersionNo(tenderDocument.getVersionNo());
            response.setStatus(TenderDocumentStatus.COMPLETED.name());
            TenderDocumentFileView compileInfoView = toView(receiptFile);
            response.setSignedFile(signedFileView);
            TenderDocumentFileView finalPackageView = toView(finalPackageFile);
            finalPackageView.setFileRole(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
            response.setFinalPackageFile(finalPackageView);
            response.setCompileInfoFile(compileInfoView);
            // 全流程结束后再标记 SUCCESS，保证记录时间与业务完成时间一致。
            tenderDocumentGenerationRecordService.markSuccess(generationRecordId, completedAt);
            log.info("TENDER_DOCUMENT GENERATE phase=finish status={} tenderDocumentId={} projectId={} tenderId={} versionNo={} recordId={} durationMs={}",
                    TenderDocumentGenerationStatus.SUCCESS.name(),
                    tenderDocumentId,
                    tenderDocument.getProjectId(),
                    tenderDocument.getTenderId(),
                    tenderDocument.getVersionNo(),
                    generationRecordId,
                    completedAt.getTime() - generationStartTime.getTime());
            return response;
        } catch (Exception ex) {
            Date failedAt = new Date();
            // 失败时单独记录错误码和错误信息，便于后续分页查询与排障。
            tenderDocumentGenerationRecordService.markFail(generationRecordId, resolveErrorCode(ex), resolveErrorMessage(ex), failedAt);
            log.error("TENDER_DOCUMENT GENERATE phase=finish status={} tenderDocumentId={} projectId={} tenderId={} versionNo={} recordId={} errorCode={} errorMessage={}",
                    TenderDocumentGenerationStatus.FAIL.name(),
                    tenderDocumentId,
                    tenderDocument.getProjectId(),
                    tenderDocument.getTenderId(),
                    tenderDocument.getVersionNo(),
                    generationRecordId,
                    resolveErrorCode(ex),
                    resolveErrorMessage(ex),
                    ex);
            resetStatusToDraft(tenderDocumentId);
            throw ex;
        }
    }

    /**
     * 单独回传签章文件，供统一回传接口内部复用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentCallbackResponse callbackSignedFile(Long tenderDocumentId, TenderDocumentCallbackRequest request,
                                                             TenderDocumentUserContext userContext) {
        return callbackFile(tenderDocumentId, request, TenderDocumentFileType.SIGNED_PDF, userContext);
    }

    /**
     * 单独回传数据包，供统一回传接口内部复用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentCallbackResponse callbackPackageFile(Long tenderDocumentId, TenderDocumentCallbackRequest request,
                                                              TenderDocumentUserContext userContext) {
        return callbackFile(tenderDocumentId, request, TenderDocumentFileType.FINAL_PACKAGE_FILE, userContext);
    }

    /**
     * 前端只调一个回传按钮，后端内部再拆成签章文件和数据包两次回传。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentUnifiedCallbackResponse callbackAll(Long tenderDocumentId, TenderDocumentCallbackRequest request,
                                                             TenderDocumentUserContext userContext) {
        String lockKey = CALLBACK_LOCK_KEY_PREFIX + tenderDocumentId;
        String lockValue = UUID.randomUUID().toString();
        boolean lockAcquired = acquireCallbackLock(lockKey, lockValue);
        if (!lockAcquired) {
            throw new BusinessException(TenderDocumentErrorCode.CALLBACK_IN_PROGRESS.getCode(),
                    TenderDocumentErrorCode.CALLBACK_IN_PROGRESS.getMessage());
        }
        try {
            TenderDocumentUnifiedCallbackResponse response = new TenderDocumentUnifiedCallbackResponse();
            // 统一回传接口内部按文件角色拆分调用，保持失败隔离并复用单项回传逻辑。
            TenderDocumentCallbackResponse signedResult = callbackSignedFile(tenderDocumentId, request == null ? new TenderDocumentCallbackRequest() : request, userContext);
            TenderDocumentCallbackResponse packageResult = callbackPackageFile(tenderDocumentId, request == null ? new TenderDocumentCallbackRequest() : request, userContext);
            response.setSignedFileResult(signedResult);
            response.setPackageFileResult(packageResult);
            boolean success = Boolean.TRUE.equals(signedResult.getSuccess()) && Boolean.TRUE.equals(packageResult.getSuccess());
            response.setSuccess(success);
            response.setMessage(success ? "回传完成" : buildUnifiedCallbackFailureMessage(signedResult, packageResult));
            return response;
        } finally {
            releaseCallbackLock(lockKey, lockValue);
        }
    }

    private String buildUnifiedCallbackFailureMessage(TenderDocumentCallbackResponse signedResult,
                                                      TenderDocumentCallbackResponse packageResult) {
        List<String> failedParts = new ArrayList<String>();
        appendFailureMessage(failedParts, signedResult, "SIGNED_PDF");
        appendFailureMessage(failedParts, packageResult, "FINAL_PACKAGE_FILE");
        if (failedParts.isEmpty()) {
            return "回传部分失败";
        }
        return "回传失败: " + String.join("; ", failedParts);
    }

    private void appendFailureMessage(List<String> failedParts, TenderDocumentCallbackResponse callbackResponse, String fileRole) {
        if (callbackResponse == null || Boolean.TRUE.equals(callbackResponse.getSuccess())) {
            return;
        }
        String role = StringUtils.hasText(callbackResponse.getFileRole()) ? callbackResponse.getFileRole() : fileRole;
        String responseCode = StringUtils.hasText(callbackResponse.getResponseCode()) ? callbackResponse.getResponseCode() : "UNKNOWN";
        String responseMessage = StringUtils.hasText(callbackResponse.getResponseMessage()) ? callbackResponse.getResponseMessage() : "未返回错误信息";
        failedParts.add(role + "(" + responseCode + "): " + responseMessage);
    }

    private boolean acquireCallbackLock(String lockKey, String lockValue) {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, CALLBACK_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(locked);
    }

    private void releaseCallbackLock(String lockKey, String lockValue) {
        try {
            stringRedisTemplate.execute(RELEASE_CALLBACK_LOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
        } catch (Exception ex) {
            log.warn("TENDER_DOCUMENT CALLBACK_LOCK_RELEASE_FAILED lockKey={} message={}", lockKey, ex.getMessage());
        }
    }

    /**
     * 回传记录按文件角色分别留痕，便于后续重试和审计。
     */
    private TenderDocumentCallbackResponse callbackFile(Long tenderDocumentId, TenderDocumentCallbackRequest request,
                                                        TenderDocumentFileType fileRole, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        if (!TenderDocumentStatus.COMPLETED.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.CALLBACK_NOT_ALLOWED.getCode(),
                    "当前编制单尚未生成完成，不能回传");
        }
        TenderDocumentScopeType scopeType = resolveScopeTypeByRole(tenderDocument, fileRole);
        String resolvedTenderId = resolveCallbackTenderId(
                scopeType,
                tenderDocument.getTenderId(),
                request == null ? null : request.getTenderId()
        );
        TenderDocumentFile file = requireActiveFile(tenderDocumentId, fileRole, scopeType, resolvedTenderId);

        TenderDocumentCallbackGatewayResult gatewayResult;
        try {
            gatewayResult = fileRole == TenderDocumentFileType.SIGNED_PDF
                    ? tenderDocumentCallbackGateway.callbackSignedFile(tenderDocument, file, resolvedTenderId, userContext)
                    : tenderDocumentCallbackGateway.callbackPackageFile(tenderDocument, file, resolvedTenderId, userContext);
        } catch (Exception ex) {
            // 统一回传场景要求“失败隔离+可留痕”，网关异常不再中断整批回传。
            gatewayResult = new TenderDocumentCallbackGatewayResult(
                    false,
                    resolveErrorCode(ex),
                    resolveErrorMessage(ex)
            );
        }
        if (gatewayResult == null) {
            gatewayResult = new TenderDocumentCallbackGatewayResult(
                    false,
                    String.valueOf(TenderDocumentErrorCode.BUSINESS_SYSTEM_INTERACTION_FAILED.getCode()),
                    "回传网关返回空结果"
            );
        }

        int nextRetryCount = increaseAndGetRetryCount(
                tenderDocumentId,
                tenderDocument.getVersionNo(),
                fileRole.name(),
                resolvedTenderId
        );

        TenderDocumentCallback callback = new TenderDocumentCallback();
        callback.setTenderDocumentId(tenderDocumentId);
        callback.setVersionNo(tenderDocument.getVersionNo());
        callback.setProjectId(tenderDocument.getProjectId());
        callback.setTenderId(resolvedTenderId);
        callback.setFileRole(fileRole.name());
        callback.setFileId(file.getFileId());
        callback.setCallbackStatus(gatewayResult.isSuccess() ? TenderDocumentCallbackStatus.SUCCESS.name() : TenderDocumentCallbackStatus.FAIL.name());
        callback.setCallbackTime(new Date());
        callback.setResponseCode(gatewayResult.getResponseCode());
        callback.setResponseMessage(gatewayResult.getResponseMessage());
        callback.setRetryCount(nextRetryCount);
        callback.setOperatorId(userContext.getUserId());
        callback.setOperatorName(userContext.getUserName());
        tenderDocumentCallbackMapper.insert(callback);

        TenderDocumentCallbackResponse response = new TenderDocumentCallbackResponse();
        response.setTenderId(resolvedTenderId);
        response.setFileRole(fileRole.name());
        response.setSuccess(gatewayResult.isSuccess());
        response.setResponseCode(gatewayResult.getResponseCode());
        response.setResponseMessage(gatewayResult.getResponseMessage());
        response.setRetryCount(nextRetryCount);
        return response;
    }

    private int increaseAndGetRetryCount(Long tenderDocumentId, Integer versionNo, String fileRole, String tenderId) {
        String normalizedTenderId = normalizeCounterTenderId(tenderId);
        tenderDocumentCallbackCounterMapper.upsertAndIncreaseRetryCount(tenderDocumentId, versionNo, fileRole, normalizedTenderId);
        Integer retryCount = tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(tenderDocumentId, versionNo, fileRole, normalizedTenderId);
        if (retryCount == null || retryCount < 1) {
            log.warn("TENDER_DOCUMENT CALLBACK_RETRY_COUNT_FALLBACK tenderDocumentId={} versionNo={} fileRole={} tenderId={}",
                    tenderDocumentId, versionNo, fileRole, tenderId);
            return 1;
        }
        return retryCount;
    }

    private String normalizeCounterTenderId(String tenderId) {
        return StringUtils.hasText(tenderId) ? tenderId : EMPTY_TENDER_ID;
    }

    /**
     * 统一做编制单存在和项目锁校验。
     */
    private TenderDocument requireDocument(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = tenderDocumentMapper.selectById(tenderDocumentId);
        if (tenderDocument == null) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getMessage());
        }
        ProjectLock ignored = projectLockService.verifyOrCreateLock(tenderDocument.getProjectId(), userContext);
        return tenderDocument;
    }

    private void assertGenerationAllowed(TenderDocument tenderDocument) {
        // GENEARTING/COMPLETED 都属于不可重复点击“生成文件”的状态。
        if (TenderDocumentStatus.GENERATING.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.GENERATION_IN_PROGRESS.getCode(),
                    TenderDocumentErrorCode.GENERATION_IN_PROGRESS.getMessage());
        }
        if (TenderDocumentStatus.COMPLETED.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.GENERATION_ALREADY_COMPLETED.getCode(),
                    TenderDocumentErrorCode.GENERATION_ALREADY_COMPLETED.getMessage());
        }
        if (!TenderDocumentStatus.DRAFT.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.GENERATION_NOT_ALLOWED.getCode(),
                    TenderDocumentErrorCode.GENERATION_NOT_ALLOWED.getMessage());
        }
    }

    private void claimGeneratingStatus(Long tenderDocumentId) {
        // 使用条件更新（status = DRAFT）实现轻量锁；只有一个请求能抢占成功。
        Integer updated = requiresNewTransactionTemplate.execute(status -> {
            TenderDocument updating = new TenderDocument();
            updating.setStatus(TenderDocumentStatus.GENERATING.name());
            return tenderDocumentMapper.update(updating, Wrappers.<TenderDocument>lambdaUpdate()
                    .eq(TenderDocument::getId, tenderDocumentId)
                    .eq(TenderDocument::getStatus, TenderDocumentStatus.DRAFT.name()));
        });
        if (updated != null && updated > 0) {
            return;
        }

        TenderDocument latestDocument = tenderDocumentMapper.selectById(tenderDocumentId);
        if (latestDocument == null) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getMessage());
        }
        assertGenerationAllowed(latestDocument);
        throw new BusinessException(TenderDocumentErrorCode.GENERATION_NOT_ALLOWED.getCode(),
                TenderDocumentErrorCode.GENERATION_NOT_ALLOWED.getMessage());
    }

    private void resetStatusToDraft(Long tenderDocumentId) {
        // 仅在当前状态仍为 GENERATING 时回退，避免覆盖并发场景下的后续正确状态。
        requiresNewTransactionTemplate.executeWithoutResult(status -> {
            TenderDocument resetting = new TenderDocument();
            resetting.setStatus(TenderDocumentStatus.DRAFT.name());
            tenderDocumentMapper.update(resetting, Wrappers.<TenderDocument>lambdaUpdate()
                    .eq(TenderDocument::getId, tenderDocumentId)
                    .eq(TenderDocument::getStatus, TenderDocumentStatus.GENERATING.name()));
        });
    }

    /**
     * 最后一步生成前，所有前置步骤都必须已经完成。
     */
    private void validateGenerationPreconditions(Long tenderDocumentId) {
        List<TenderDocumentStep> steps = tenderDocumentStepMapper.selectList(Wrappers.<TenderDocumentStep>lambdaQuery()
                .eq(TenderDocumentStep::getTenderDocumentId, tenderDocumentId));
        List<TenderDocumentStepCode> requiredSteps = List.of(
                TenderDocumentStepCode.BASIC_INFO,
                TenderDocumentStepCode.PURCHASE_FILE,
                TenderDocumentStepCode.BID_RECORD,
                TenderDocumentStepCode.EVALUATION_RULE,
                TenderDocumentStepCode.CHECK_ITEMS
        );
        for (TenderDocumentStepCode requiredStep : requiredSteps) {
            boolean completed = steps.stream().anyMatch(step ->
                    requiredStep.name().equals(step.getStepCode())
                            && TenderDocumentStepStatus.COMPLETED.name().equals(step.getStepStatus()));
            if (!completed) {
                throw new BusinessException(TenderDocumentErrorCode.GENERATION_NOT_ALLOWED.getCode(),
                        "步骤" + requiredStep.name() + "尚未完成，不能生成文件");
            }
        }
    }

    /**
     * 编制信息 PDF 始终按项目级存储，其余产物默认跟随编制单粒度。
     */
    private TenderDocumentScopeType resolveScopeType(TenderDocument tenderDocument) {
        return StringUtils.hasText(tenderDocument.getCompileScope()) && TenderDocumentScopeType.TENDER.name().equalsIgnoreCase(tenderDocument.getCompileScope())
                ? TenderDocumentScopeType.TENDER
                : TenderDocumentScopeType.PROJECT;
    }

    private TenderDocumentScopeType resolveScopeTypeByRole(TenderDocument tenderDocument, TenderDocumentFileType fileRole) {
        if (fileRole == TenderDocumentFileType.COMPILE_INFO_PDF) {
            return TenderDocumentScopeType.PROJECT;
        }
        if (fileRole == TenderDocumentFileType.FINAL_PACKAGE_FILE) {
            return resolveScopeType(tenderDocument);
        }
        return resolveScopeType(tenderDocument);
    }

    /**
     * 邀请类生成和回传都需要明确标段ID，公开类统一返回空。
     */
    private String resolveTenderId(TenderDocumentScopeType scopeType, String tenderId) {
        if (scopeType == TenderDocumentScopeType.TENDER) {
            if (!StringUtils.hasText(tenderId)) {
                throw new BusinessException(TenderDocumentErrorCode.TENDER_ID_REQUIRED.getCode(),
                        TenderDocumentErrorCode.TENDER_ID_REQUIRED.getMessage());
            }
            return tenderId;
        }
        return null;
    }

    /**
     * 回传场景下，标段级必须明确 tenderId；项目级优先透传请求 tenderId，缺失时再回退编制单 tenderId。
     */
    private String resolveCallbackTenderId(TenderDocumentScopeType scopeType, String documentTenderId, String requestTenderId) {
        if (scopeType == TenderDocumentScopeType.TENDER) {
            if (!StringUtils.hasText(requestTenderId)) {
                throw new BusinessException(TenderDocumentErrorCode.TENDER_ID_REQUIRED.getCode(),
                        TenderDocumentErrorCode.TENDER_ID_REQUIRED.getMessage());
            }
            return requestTenderId;
        }
        if (StringUtils.hasText(requestTenderId)) {
            return requestTenderId;
        }
        return StringUtils.hasText(documentTenderId) ? documentTenderId : null;
    }

    /**
     * 生成和回传都只允许使用当前有效文件，避免拿到历史失效产物。
     */
    private TenderDocumentFile requireActiveFile(Long tenderDocumentId, TenderDocumentFileType fileRole,
                                                 TenderDocumentScopeType scopeType, String tenderId) {
        TenderDocumentFile file = tenderDocumentFileMapper.selectOne(Wrappers.<TenderDocumentFile>lambdaQuery()
                .eq(TenderDocumentFile::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentFile::getScopeType, scopeType.name())
                .eq(TenderDocumentFile::getFileRole, fileRole.name())
                .eq(TenderDocumentFile::getActiveFlag, 1)
                .eq(scopeType == TenderDocumentScopeType.TENDER, TenderDocumentFile::getTenderId, tenderId)
                .isNull(scopeType == TenderDocumentScopeType.PROJECT, TenderDocumentFile::getTenderId)
                .last("limit 1"));
        if (file == null) {
            throw new BusinessException(TenderDocumentErrorCode.GENERATED_FILE_MISSING.getCode(),
                    "缺少可用的" + fileRole.name() + "文件");
        }
        return file;
    }

    private FinalPackagePayload buildFinalPackagePayload(TenderDocument tenderDocument,
                                                         TenderDocumentFile signedPdfFile,
                                                         TenderDocumentUserContext userContext) {
        FinalPackagePayload payload = finalPackageDataAssembler.assemble(tenderDocument);
        // 数据包只嵌入“签章后 PDF”，不再附带未签章源文件内容。
        payload.setTenderDocumentSignPdf(signedPdfContentLoader.load(signedPdfFile));
        payload.setCaKeysInfo(resolveCaKeysInfo(tenderDocument, userContext));
        if (payload.getSettings() == null) {
            FinalPackagePayload.Settings settings = new FinalPackagePayload.Settings();
            FinalPackagePayload.SignPosition signPosition = new FinalPackagePayload.SignPosition();
            signPosition.setX(200);
            signPosition.setY(200);
            settings.setSignPosition(signPosition);
            payload.setSettings(settings);
        }
        if (payload.getVersionInfo() == null) {
            FinalPackagePayload.VersionInfo versionInfo = new FinalPackagePayload.VersionInfo();
            versionInfo.setTenderDocumentFormatVersion(tenderDocumentVersionProperties.getFormatVersion());
            versionInfo.setTenderDocumentAppVersion(String.valueOf(tenderDocumentVersionProperties.getAppVersion()));
            versionInfo.setTenderDocumentUniqueCode(tenderDocumentUniqueCodeGenerator.generate());
            payload.setVersionInfo(versionInfo);
        }
        return payload;
    }

    /**
     * 每次生成都实时从业务系统拉取“开标时有效”的 CA 锁信息，不使用历史缓存。
     */
    private List<FinalPackagePayload.CaKeyInfo> resolveCaKeysInfo(TenderDocument tenderDocument,
                                                                  TenderDocumentUserContext userContext) {
        CaKeysInfoQueryRequest request = new CaKeysInfoQueryRequest();
        request.setProjectId(tenderDocument.getProjectId());
        request.setTenderId(tenderDocument.getTenderId());
        CaKeysInfoResponse response = businessSystemRemoteClient.queryCaKeysInfo(
                userContext.getAppKey(),
                TraceContext.getTraceId(),
                userContext.getAuthorization(),
                request
        );
        List<CaKeysInfoResponse.CaKeyInfo> source = response == null ? null : response.getCaKeysInfo();
        if (source == null || source.isEmpty()) {
            throw new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_INTERACTION_FAILED.getCode(),
                    "业务系统未返回开标时有效的caKeysInfo，不能生成文件");
        }
        List<FinalPackagePayload.CaKeyInfo> result = new ArrayList<FinalPackagePayload.CaKeyInfo>();
        for (CaKeysInfoResponse.CaKeyInfo item : source) {
            if (item == null) {
                continue;
            }
            FinalPackagePayload.CaKeyInfo caKeyInfo = new FinalPackagePayload.CaKeyInfo();
            caKeyInfo.setEncryptOrder(item.getEncryptOrder());
            caKeyInfo.setUserId(item.getUserId());
            caKeyInfo.setCaId(item.getCaId());
            caKeyInfo.setCaNo(item.getCaNo());
            caKeyInfo.setPublicKey(item.getPublicKey());
            result.add(caKeyInfo);
        }
        if (result.isEmpty()) {
            throw new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_INTERACTION_FAILED.getCode(),
                    "业务系统未返回开标时有效的caKeysInfo，不能生成文件");
        }
        return result;
    }

    private String resolveTenderDocumentSuffix(TenderDocumentUserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getAppKey())) {
            return FileConstants.DEFAULT_TENDER_DOCUMENT_SUFFIX;
        }
        SysAccessSystem system = externalSystemAccessMapper.selectByAppKey(userContext.getAppKey());
        if (system != null && StringUtils.hasText(system.getTenderDocumentSuffix())) {
            return system.getTenderDocumentSuffix();
        }
        return FileConstants.DEFAULT_TENDER_DOCUMENT_SUFFIX;
    }

    private byte[] encryptFinalPackage(FinalPackagePayload finalPackagePayload) {
        try {
            // 统一先序列化为 JSON 明文，再交给加密器做二进制加密封装。
            return finalPackageEncryptor.encrypt(objectMapper.writeValueAsBytes(finalPackagePayload));
        } catch (Exception ex) {
            throw new BusinessException("最终数据包序列化失败: " + ex.getMessage());
        }
    }

    private void deactivateGeneratedFiles(Long tenderDocumentId, TenderDocumentFileType fileRole) {
        List<TenderDocumentFile> files = tenderDocumentFileMapper.selectList(Wrappers.<TenderDocumentFile>lambdaQuery()
                .eq(TenderDocumentFile::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentFile::getFileRole, fileRole.name())
                .eq(TenderDocumentFile::getActiveFlag, 1));
        // 新版本产物入库前先逻辑失效旧版本同角色文件，避免“同角色多条生效”。
        for (TenderDocumentFile file : files) {
            file.setActiveFlag(0);
            tenderDocumentFileMapper.updateById(file);
        }
    }

    private TenderDocumentFile toEntity(TenderDocument tenderDocument, TenderDocumentGeneratedFile file) {
        TenderDocumentFile entity = new TenderDocumentFile();
        entity.setTenderDocumentId(tenderDocument.getId());
        entity.setProjectId(tenderDocument.getProjectId());
        entity.setTenderId(file.getTenderId());
        entity.setScopeType(file.getScopeType());
        entity.setFileRole(file.getFileRole());
        entity.setFileId(file.getFileId());
        entity.setFileName(file.getFileName());
        entity.setFileSize(file.getFileSize());
        entity.setContentType(file.getContentType());
        entity.setFileSha256(file.getFileSha256());
        entity.setActiveFlag(1);
        entity.setCreatedTime(new Date());
        return entity;
    }

    private TenderDocumentFileView toView(TenderDocumentGeneratedFile file) {
        TenderDocumentFileView view = new TenderDocumentFileView();
        view.setTenderId(file.getTenderId());
        view.setTenderName(file.getTenderId());
        view.setScopeType(file.getScopeType());
        view.setFileRole(file.getFileRole());
        view.setFileId(file.getFileId());
        view.setFileName(file.getFileName());
        view.setFileSize(file.getFileSize());
        view.setContentType(file.getContentType());
        view.setFileSha256(file.getFileSha256());
        return view;
    }

    private TenderDocumentFileView toView(TenderDocumentFile file) {
        TenderDocumentFileView view = new TenderDocumentFileView();
        view.setTenderId(file.getTenderId());
        view.setTenderName(file.getTenderId());
        view.setScopeType(file.getScopeType());
        view.setFileRole(file.getFileRole());
        view.setFileId(file.getFileId());
        view.setFileName(file.getFileName());
        view.setFileSize(file.getFileSize());
        view.setContentType(file.getContentType());
        view.setFileSha256(file.getFileSha256());
        return view;
    }

    private TenderDocumentGenerationRecordView toView(TenderDocumentGenerationRecord record) {
        if (record == null) {
            return null;
        }
        TenderDocumentGenerationRecordView view = new TenderDocumentGenerationRecordView();
        view.setId(record.getId());
        view.setVersionNo(record.getVersionNo());
        view.setProjectId(record.getProjectId());
        view.setTenderId(record.getTenderId());
        view.setGenerateStatus(record.getGenerateStatus());
        view.setStartTime(record.getStartTime());
        view.setEndTime(record.getEndTime());
        view.setDurationMs(record.getDurationMs());
        view.setErrorCode(record.getErrorCode());
        view.setErrorMessage(record.getErrorMessage());
        view.setTraceId(record.getTraceId());
        view.setOperatorId(record.getOperatorId());
        view.setOperatorName(record.getOperatorName());
        return view;
    }

    private String writeSummaryJson(List<TenderDocumentGeneratedFile> packageFiles) {
        try {
            // 版本摘要仅做追溯用途，不参与业务系统回传。
            return objectMapper.writeValueAsString(packageFiles);
        } catch (Exception ex) {
            throw new BusinessException("生成版本摘要失败: " + ex.getMessage());
        }
    }

    private void saveCaKeysSnapshot(TenderDocument tenderDocument,
                                    TenderDocumentUserContext userContext,
                                    Date capturedTime,
                                    List<FinalPackagePayload.CaKeyInfo> caKeysInfo) {
        TenderDocumentCaKeysSnapshot snapshot = new TenderDocumentCaKeysSnapshot();
        snapshot.setTenderDocumentId(tenderDocument.getId());
        snapshot.setVersionNo(tenderDocument.getVersionNo());
        snapshot.setProjectId(tenderDocument.getProjectId());
        snapshot.setTenderId(tenderDocument.getTenderId());
        snapshot.setSourceAppKey(userContext == null ? null : userContext.getAppKey());
        snapshot.setTraceId(TraceContext.getTraceId());
        snapshot.setCapturedTime(capturedTime);
        snapshot.setCaKeysCount(caKeysInfo == null ? 0 : caKeysInfo.size());
        snapshot.setCaKeysJson(writeCaKeysSnapshotJson(caKeysInfo));
        tenderDocumentCaKeysSnapshotMapper.insert(snapshot);
    }

    private String writeCaKeysSnapshotJson(List<FinalPackagePayload.CaKeyInfo> caKeysInfo) {
        try {
            return objectMapper.writeValueAsString(caKeysInfo);
        } catch (Exception ex) {
            throw new BusinessException("caKeysInfo快照序列化失败: " + ex.getMessage());
        }
    }

    private String resolveErrorCode(Exception ex) {
        if (ex instanceof BusinessException businessException) {
            return String.valueOf(businessException.getCode());
        }
        return "INTERNAL_ERROR";
    }

    private String resolveErrorMessage(Exception ex) {
        String message = ex.getMessage();
        return StringUtils.hasText(message) ? message : ex.getClass().getSimpleName();
    }
}
