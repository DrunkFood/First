package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEntryResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentOverviewResponse;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.entity.TenderDocumentGenerationRecord;
import com.jy.eletender.tenderdocument.entity.TenderDocumentStep;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentGenerationStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepStatus;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentStepMapper;
import com.jy.eletender.tenderdocument.service.impl.TenderDocumentServiceImpl;
import com.jy.eletender.tenderdocument.support.TenderDocumentBasicInfoSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentSyncGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderDocumentServiceTest {

    @Mock
    private IProjectLockService projectLockService;

    @Mock
    private TenderDocumentMapper tenderDocumentMapper;

    @Mock
    private TenderDocumentStepMapper tenderDocumentStepMapper;

    @Mock
    private TenderDocumentFileMapper tenderDocumentFileMapper;

    @Mock
    private TenderDocumentSyncGateway tenderDocumentSyncGateway;

    @Mock
    private ITenderDocumentGenerationRecordService tenderDocumentGenerationRecordService;

    private TenderDocumentServiceImpl tenderDocumentService;

    @BeforeEach
    void setUp() {
        tenderDocumentService = new TenderDocumentServiceImpl(
                projectLockService, tenderDocumentMapper, tenderDocumentStepMapper,
                tenderDocumentFileMapper, tenderDocumentSyncGateway,
                tenderDocumentGenerationRecordService, 1800L);
    }

    // ── entry: 无记录时新建首版草稿 ──

    @Test
    void shouldCreateDraftAndInitializeStepsWhenProjectHasNoDraft() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoResult());
        when(tenderDocumentMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            TenderDocument tenderDocument = invocation.getArgument(0);
            assertThat(tenderDocument.getCompileScope()).isEqualTo(TenderDocumentScopeType.PROJECT.name());
            assertThat(tenderDocument.getTenderId()).isEqualTo("T-01");
            assertThat(tenderDocument.getIndexOf()).isEqualTo(2);
            assertThat(tenderDocument.getVersionNo()).isEqualTo(1);
            tenderDocument.setId(100L);
            return 1;
        }).when(tenderDocumentMapper).insert(any(TenderDocument.class));

        TenderDocumentEntryResponse response = tenderDocumentService.enter(request, userContext);

        assertThat(response.getTenderDocumentId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(TenderDocumentStatus.DRAFT.name());
        assertThat(response.getCurrentStepCode()).isEqualTo(TenderDocumentStepCode.BASIC_INFO.name());
        verify(tenderDocumentStepMapper, times(TenderDocumentStepCode.values().length)).insert(any(TenderDocumentStep.class));
    }

    // ── entry: 命中 DRAFT 时复用并刷新 sync ──

    @Test
    void shouldReuseExistingDraft() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument existing = new TenderDocument();
        existing.setId(88L);
        existing.setProjectId("P-100");
        existing.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        existing.setStatus(TenderDocumentStatus.DRAFT.name());
        existing.setCurrentStepCode(TenderDocumentStepCode.BID_RECORD.name());
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoResult());
        when(tenderDocumentMapper.selectOne(any())).thenReturn(existing);

        TenderDocumentEntryResponse response = tenderDocumentService.enter(request, userContext);

        assertThat(response.getTenderDocumentId()).isEqualTo(88L);
        assertThat(response.getCurrentStepCode()).isEqualTo(TenderDocumentStepCode.BID_RECORD.name());
        verify(tenderDocumentMapper, times(1)).updateById(existing);
        verify(tenderDocumentMapper, never()).insert(any(TenderDocument.class));
    }

    // ── entry: 命中 COMPLETED 时原样返回、不刷新 sync ──

    @Test
    void shouldReturnCompletedDocumentAsIs() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument completed = buildCompletedDocument();
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoResult());
        when(tenderDocumentMapper.selectOne(any())).thenReturn(completed);

        TenderDocumentEntryResponse response = tenderDocumentService.enter(request, userContext);

        assertThat(response.getTenderDocumentId()).isEqualTo(89L);
        assertThat(response.getStatus()).isEqualTo(TenderDocumentStatus.COMPLETED.name());
        verify(tenderDocumentMapper, never()).updateById(any());
        verify(tenderDocumentMapper, never()).insert(any(TenderDocument.class));
    }

    // ── entry: GENERATING 超时 → 重置为 DRAFT ──

    @Test
    void shouldResetGeneratingToDrawWhenTimedOut() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument generating = new TenderDocument();
        generating.setId(90L);
        generating.setProjectId("P-100");
        generating.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        generating.setStatus(TenderDocumentStatus.GENERATING.name());
        generating.setCurrentStepCode(TenderDocumentStepCode.GENERATE_PACKAGE.name());
        generating.setModifyTime(new Date(System.currentTimeMillis() - 3600_000)); // 1小时前
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoResult());
        when(tenderDocumentMapper.selectOne(any())).thenReturn(generating);
        when(tenderDocumentGenerationRecordService.findLatestByTenderDocumentId(90L)).thenReturn(null);

        TenderDocumentEntryResponse response = tenderDocumentService.enter(request, userContext);

        assertThat(response.getStatus()).isEqualTo(TenderDocumentStatus.DRAFT.name());
        verify(tenderDocumentMapper, times(1)).updateById(generating);
    }

    // ── entry: GENERATING 未超时 → 原样返回 ──

    @Test
    void shouldReturnGeneratingAsIsWhenNotTimedOut() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument generating = new TenderDocument();
        generating.setId(90L);
        generating.setProjectId("P-100");
        generating.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        generating.setStatus(TenderDocumentStatus.GENERATING.name());
        generating.setCurrentStepCode(TenderDocumentStepCode.GENERATE_PACKAGE.name());
        generating.setModifyTime(new Date()); // 刚刚
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoResult());
        when(tenderDocumentMapper.selectOne(any())).thenReturn(generating);

        TenderDocumentEntryResponse response = tenderDocumentService.enter(request, userContext);

        assertThat(response.getStatus()).isEqualTo(TenderDocumentStatus.GENERATING.name());
        verify(tenderDocumentMapper, never()).updateById(any());
    }

    // ── entry: GENERATING 超时且存在 PROCESSING 生成记录 → 标记为失败 ──

    @Test
    void shouldMarkProcessingRecordAsFailWhenGenerationTimedOut() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument generating = new TenderDocument();
        generating.setId(90L);
        generating.setProjectId("P-100");
        generating.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        generating.setStatus(TenderDocumentStatus.GENERATING.name());
        generating.setCurrentStepCode(TenderDocumentStepCode.GENERATE_PACKAGE.name());
        generating.setModifyTime(new Date(System.currentTimeMillis() - 3600_000));
        TenderDocumentGenerationRecord processingRecord = new TenderDocumentGenerationRecord();
        processingRecord.setId(50L);
        processingRecord.setGenerateStatus(TenderDocumentGenerationStatus.PROCESSING.name());
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoResult());
        when(tenderDocumentMapper.selectOne(any())).thenReturn(generating);
        when(tenderDocumentGenerationRecordService.findLatestByTenderDocumentId(90L)).thenReturn(processingRecord);

        tenderDocumentService.enter(request, userContext);

        verify(tenderDocumentGenerationRecordService).markFail(eq(50L), eq("GENERATION_TIMEOUT"), any(), any());
    }

    // ── recompile: 正常重新编制 ──

    @Test
    void shouldRecompileCompletedDocument() {
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument completed = buildCompletedDocument();
        when(tenderDocumentMapper.selectById(89L)).thenReturn(completed);
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        TenderDocumentStep generateStep = buildStep(89L, TenderDocumentStepCode.GENERATE_PACKAGE, TenderDocumentStepStatus.COMPLETED);
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(generateStep);
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of());

        TenderDocumentEntryResponse response = tenderDocumentService.recompile(89L, userContext);

        assertThat(response.getStatus()).isEqualTo(TenderDocumentStatus.DRAFT.name());
        assertThat(response.getVersionNo()).isEqualTo(3);
        verify(tenderDocumentMapper).updateById(completed);
        assertThat(generateStep.getStepStatus()).isEqualTo(TenderDocumentStepStatus.IN_PROGRESS.name());
        verify(tenderDocumentStepMapper).updateById(generateStep);
    }

    // ── recompile: 失效旧产物文件 ──

    @Test
    void shouldDeactivateGeneratedFilesOnRecompile() {
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument completed = buildCompletedDocument();
        when(tenderDocumentMapper.selectById(89L)).thenReturn(completed);
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(
                buildStep(89L, TenderDocumentStepCode.GENERATE_PACKAGE, TenderDocumentStepStatus.COMPLETED));
        TenderDocumentFile packageFile = new TenderDocumentFile();
        packageFile.setActiveFlag(1);
        TenderDocumentFile compileInfoFile = new TenderDocumentFile();
        compileInfoFile.setActiveFlag(1);
        when(tenderDocumentFileMapper.selectList(any()))
                .thenReturn(List.of(packageFile))
                .thenReturn(List.of(compileInfoFile));

        tenderDocumentService.recompile(89L, userContext);

        assertThat(packageFile.getActiveFlag()).isEqualTo(0);
        assertThat(compileInfoFile.getActiveFlag()).isEqualTo(0);
        verify(tenderDocumentFileMapper, times(2)).updateById(any(TenderDocumentFile.class));
    }

    // ── recompile: 非 COMPLETED 状态拒绝 ──

    @Test
    void shouldRejectRecompileWhenNotCompleted() {
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument draft = new TenderDocument();
        draft.setId(89L);
        draft.setProjectId("P-100");
        draft.setStatus(TenderDocumentStatus.DRAFT.name());
        when(tenderDocumentMapper.selectById(89L)).thenReturn(draft);
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());

        assertThatThrownBy(() -> tenderDocumentService.recompile(89L, userContext))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(TenderDocumentErrorCode.RECOMPILE_NOT_ALLOWED.getMessage());
    }

    // ── overview ──

    @Test
    void shouldReturnDocumentOverview() {
        TenderDocumentUserContext userContext = buildContext();
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(99L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setStatus(TenderDocumentStatus.DRAFT.name());
        tenderDocument.setCurrentStepCode(TenderDocumentStepCode.PURCHASE_FILE.name());
        when(tenderDocumentMapper.selectById(99L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(List.of(buildStep(99L, TenderDocumentStepCode.BASIC_INFO, TenderDocumentStepStatus.COMPLETED),
                buildStep(99L, TenderDocumentStepCode.PURCHASE_FILE, TenderDocumentStepStatus.IN_PROGRESS)));

        TenderDocumentOverviewResponse response = tenderDocumentService.getOverview(99L, userContext);

        assertThat(response.getTenderDocumentId()).isEqualTo(99L);
        assertThat(response.getStepList()).hasSize(2);
    }

    // ── entry: compileScope 解析 ──

    @Test
    void shouldResolveTenderScopeFromProjectTypeWhenCompileScopeMissing() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        TenderDocumentBasicInfoSyncResult syncResult = buildBasicInfoResult();
        syncResult.setCompileScope(null);
        syncResult.setProjectType(InteractionProjectType.INVITE);
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(syncResult);
        when(tenderDocumentMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            TenderDocument tenderDocument = invocation.getArgument(0);
            assertThat(tenderDocument.getCompileScope()).isEqualTo(TenderDocumentScopeType.TENDER.name());
            assertThat(tenderDocument.getTenderId()).isEqualTo("T-01");
            tenderDocument.setId(101L);
            return 1;
        }).when(tenderDocumentMapper).insert(any(TenderDocument.class));

        TenderDocumentEntryResponse response = tenderDocumentService.enter(request, userContext);

        assertThat(response.getTenderDocumentId()).isEqualTo(101L);
        assertThat(response.getTenderId()).isEqualTo("T-01");
    }

    @Test
    void shouldRejectEntryWhenScopeCannotBeResolved() {
        TenderDocumentEntryRequest request = buildEntryRequest();
        TenderDocumentUserContext userContext = buildContext();
        TenderDocumentBasicInfoSyncResult syncResult = buildBasicInfoResult();
        syncResult.setCompileScope(null);
        syncResult.setProjectType(null);
        when(projectLockService.verifyOrCreateLock("P-100", userContext)).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(syncResult);

        assertThatThrownBy(() -> tenderDocumentService.enter(request, userContext))
                .hasMessageContaining("projectType");
    }

    // ── helpers ──

    private TenderDocument buildCompletedDocument() {
        TenderDocument completed = new TenderDocument();
        completed.setId(89L);
        completed.setProjectId("P-100");
        completed.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        completed.setStatus(TenderDocumentStatus.COMPLETED.name());
        completed.setCurrentStepCode(TenderDocumentStepCode.GENERATE_PACKAGE.name());
        completed.setVersionNo(2);
        return completed;
    }

    private TenderDocumentEntryRequest buildEntryRequest() {
        TenderDocumentEntryRequest request = new TenderDocumentEntryRequest();
        request.setBizType(1);
        request.setBizId("BIZ-100");
        request.setProjectId("P-100");
        request.setTenderId("T-01");
        return request;
    }

    private TenderDocumentBasicInfoSyncResult buildBasicInfoResult() {
        TenderDocumentBasicInfoSyncResult result = new TenderDocumentBasicInfoSyncResult();
        result.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        result.setProjectType(InteractionProjectType.PUBLIC);
        result.setProjectCode("PJT-001");
        result.setProjectName("项目一");
        result.setPurchaseMethod("公开招标");
        result.setEvalMethod(InteractionEvalMethod.COMPREHENSIVE_SCORE);
        result.setTenderList(List.of(
                Map.of("tenderId", "T-99", "indexOf", 1),
                Map.of("tenderId", "T-01", "indexOf", 2)
        ));
        return result;
    }

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("app-a");
        context.setUserId("u-1");
        context.setUserName("测试用户");
        context.setEnterpriseId("ent-1");
        context.setEnterpriseName("测试企业");
        context.setEnterpriseCode("913301");
        return context;
    }

    private TenderDocumentStep buildStep(Long tenderDocumentId, TenderDocumentStepCode stepCode, TenderDocumentStepStatus stepStatus) {
        TenderDocumentStep step = new TenderDocumentStep();
        step.setTenderDocumentId(tenderDocumentId);
        step.setStepCode(stepCode.name());
        step.setStepStatus(stepStatus.name());
        step.setStepOrder(stepCode.getStepOrder());
        return step;
    }
}
