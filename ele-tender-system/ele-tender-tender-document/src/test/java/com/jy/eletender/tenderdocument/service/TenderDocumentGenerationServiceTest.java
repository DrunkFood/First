package com.jy.eletender.tenderdocument.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.interaction.dto.CaKeysInfoResponse;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentCallbackRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCallbackResponse;
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
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepStatus;
import com.jy.eletender.tenderdocument.mapper.ExternalSystemAccessMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentCallbackMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentCallbackCounterMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentCaKeysSnapshotMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentStepMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentVersionMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.model.generation.FinalPackagePayload;
import com.jy.eletender.tenderdocument.service.impl.TenderDocumentGenerationServiceImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderDocumentGenerationServiceTest {

    @Mock
    private TenderDocumentMapper tenderDocumentMapper;

    @Mock
    private TenderDocumentStepMapper tenderDocumentStepMapper;

    @Mock
    private TenderDocumentFileMapper tenderDocumentFileMapper;

    @Mock
    private TenderDocumentCaKeysSnapshotMapper tenderDocumentCaKeysSnapshotMapper;

    @Mock
    private TenderDocumentVersionMapper tenderDocumentVersionMapper;

    @Mock
    private TenderDocumentCallbackMapper tenderDocumentCallbackMapper;

    @Mock
    private TenderDocumentCallbackCounterMapper tenderDocumentCallbackCounterMapper;

    @Mock
    private TenderRuleHeaderMapper tenderRuleHeaderMapper;

    @Mock
    private IProjectLockService projectLockService;

    @Mock
    private TenderDocumentGenerationGateway tenderDocumentGenerationGateway;

    @Mock
    private TenderDocumentCallbackGateway tenderDocumentCallbackGateway;

    @Mock
    private CompileInfoPdfGenerator compileInfoPdfGenerator;

    @Mock
    private FinalPackageDataAssembler finalPackageDataAssembler;

    @Mock
    private SignedPdfContentLoader signedPdfContentLoader;

    @Mock
    private FinalPackageEncryptor finalPackageEncryptor;

    @Mock
    private TenderDocumentUniqueCodeGenerator tenderDocumentUniqueCodeGenerator;

    @Mock
    private ITenderDocumentGenerationRecordService tenderDocumentGenerationRecordService;

    @Mock
    private BusinessSystemRemoteClient businessSystemRemoteClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ExternalSystemAccessMapper externalSystemAccessMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final PlatformTransactionManager transactionManager = new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            // no-op for unit test
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            // no-op for unit test
        }
    };

    private TenderDocumentGenerationServiceImpl tenderDocumentGenerationService;
    private TenderDocumentVersionProperties versionProperties;

    @BeforeEach
    void setUp() {
        versionProperties = new TenderDocumentVersionProperties();
        versionProperties.setAppVersion(35);
        versionProperties.setFormatVersion("2.1_35_SZCT");
        tenderDocumentGenerationService = new TenderDocumentGenerationServiceImpl(
                tenderDocumentMapper,
                tenderDocumentStepMapper,
                tenderDocumentFileMapper,
                tenderDocumentCaKeysSnapshotMapper,
                tenderDocumentVersionMapper,
                tenderDocumentCallbackMapper,
                tenderDocumentCallbackCounterMapper,
                tenderRuleHeaderMapper,
                projectLockService,
                tenderDocumentGenerationGateway,
                tenderDocumentCallbackGateway,
                compileInfoPdfGenerator,
                finalPackageDataAssembler,
                signedPdfContentLoader,
                finalPackageEncryptor,
                tenderDocumentUniqueCodeGenerator,
                versionProperties,
                tenderDocumentGenerationRecordService,
                businessSystemRemoteClient,
                objectMapper,
                stringRedisTemplate,
                transactionManager,
                externalSystemAccessMapper
        );
        lenient().when(tenderDocumentGenerationRecordService.createProcessingRecord(any(), any()))
                .thenReturn(buildGenerationRecord(900L));
        lenient().when(tenderDocumentGenerationRecordService.findLatestByTenderDocumentId(any()))
                .thenReturn(null);
        lenient().when(tenderDocumentGenerationRecordService.listByTenderDocumentId(any()))
                .thenReturn(List.of());
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(
                        startsWith("tender-document:callback:lock:"),
                        anyString(),
                        anyLong(),
                        eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        lenient().when(tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(anyLong(), anyInt(), anyString(), anyString()))
                .thenReturn(1);
    }

    @Test
    void shouldGenerateProjectScopedArtifactsForPublicProject() throws Exception {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        TenderDocumentFile purchaseFile = buildFile(TenderDocumentFileType.PURCHASE_SOURCE_PDF, TenderDocumentScopeType.PROJECT, null, 10L);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.PROJECT, null, 11L);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(buildCompletedSteps());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(
                purchaseFile,
                signedFile
        ));
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(purchaseFile, signedFile);
        mockFinalPackagePipeline(tenderDocument, signedFile);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"packageCount\":1}");
        when(tenderDocumentGenerationGateway.saveFinalPackage(eq(tenderDocument), eq(TenderDocumentScopeType.PROJECT), eq(null), any(), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.PROJECT, null, 200L, "项目包.HzctZbs"));
        when(tenderDocumentGenerationGateway.saveCompileInfoPdf(eq(tenderDocument), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.COMPILE_INFO_PDF, TenderDocumentScopeType.PROJECT, null, 201L, "编制信息.pdf"));

        TenderDocumentGenerateResponse response = tenderDocumentGenerationService.generate(100L, buildContext());

        assertThat(response.getStatus()).isEqualTo(TenderDocumentStatus.COMPLETED.name());
        assertThat(response.getSignedFile()).isNotNull();
        assertThat(response.getFinalPackageFile()).isNotNull();
        assertThat(response.getFinalPackageFile().getFileRole()).isEqualTo(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
        assertThat(response.getCompileInfoFile()).isNotNull();
        assertThat(response.getCompileInfoFile().getFileRole()).isEqualTo(TenderDocumentFileType.COMPILE_INFO_PDF.name());
        ArgumentCaptor<TenderDocumentCaKeysSnapshot> caKeysSnapshotCaptor = ArgumentCaptor.forClass(TenderDocumentCaKeysSnapshot.class);
        verify(tenderDocumentCaKeysSnapshotMapper).insert(caKeysSnapshotCaptor.capture());
        assertThat(caKeysSnapshotCaptor.getValue().getTenderDocumentId()).isEqualTo(100L);
        assertThat(caKeysSnapshotCaptor.getValue().getVersionNo()).isEqualTo(2);
        verify(tenderDocumentVersionMapper).insert(any(TenderDocumentVersion.class));
        verify(tenderDocumentFileMapper, times(2)).insert(any(TenderDocumentFile.class));
        verify(tenderDocumentMapper).update(any(TenderDocument.class), any());
        verify(tenderDocumentMapper).updateById(any(TenderDocument.class));
        verify(tenderDocumentGenerationRecordService).createProcessingRecord(eq(tenderDocument), any(TenderDocumentUserContext.class));
        verify(tenderDocumentGenerationRecordService).markSuccess(eq(900L), any(Date.class));
        verify(tenderDocumentGenerationRecordService, never()).markFail(any(), any(), any(), any(Date.class));
    }

    @Test
    void shouldGenerateTenderScopedArtifactsForInvitedProject() throws Exception {
        TenderDocument tenderDocument = buildDocument("邀请招标", TenderDocumentStatus.DRAFT);
        TenderDocumentFile purchaseFile = buildFile(TenderDocumentFileType.PURCHASE_SOURCE_PDF, TenderDocumentScopeType.TENDER, "T-01", 10L);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.TENDER, "T-01", 11L);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(buildCompletedSteps());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(
                purchaseFile,
                signedFile
        ));
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(purchaseFile, signedFile);
        mockFinalPackagePipeline(tenderDocument, signedFile);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"packageCount\":1}");
        when(tenderDocumentGenerationGateway.saveFinalPackage(eq(tenderDocument), eq(TenderDocumentScopeType.TENDER), eq("T-01"), any(), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.TENDER, "T-01", 300L, "一标段包.HzctZbs"));
        when(tenderDocumentGenerationGateway.saveCompileInfoPdf(eq(tenderDocument), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.COMPILE_INFO_PDF, TenderDocumentScopeType.PROJECT, null, 301L, "编制信息.pdf"));

        TenderDocumentGenerateResponse response = tenderDocumentGenerationService.generate(100L, buildContext());

        assertThat(response.getFinalPackageFile()).isNotNull();
        assertThat(response.getFinalPackageFile().getTenderId()).isEqualTo("T-01");
    }

    @Test
    void shouldUseConfiguredVersionInfoWhenBuildingFinalPackagePayload() throws Exception {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        TenderDocumentFile purchaseFile = buildFile(TenderDocumentFileType.PURCHASE_SOURCE_PDF, TenderDocumentScopeType.PROJECT, null, 10L);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.PROJECT, null, 11L);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(buildCompletedSteps());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(purchaseFile, signedFile));
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(purchaseFile, signedFile);
        mockFinalPackagePipeline(tenderDocument, signedFile);
        when(tenderDocumentUniqueCodeGenerator.generate()).thenReturn("UNIQUE-CODE-001");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"packageCount\":1}");
        when(tenderDocumentGenerationGateway.saveFinalPackage(eq(tenderDocument), eq(TenderDocumentScopeType.PROJECT), eq(null), any(), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.PROJECT, null, 200L, "项目包.HzctZbs"));
        when(tenderDocumentGenerationGateway.saveCompileInfoPdf(eq(tenderDocument), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.COMPILE_INFO_PDF, TenderDocumentScopeType.PROJECT, null, 201L, "编制信息.pdf"));

        tenderDocumentGenerationService.generate(100L, buildContext());

        ArgumentCaptor<FinalPackagePayload> payloadCaptor = ArgumentCaptor.forClass(FinalPackagePayload.class);
        verify(objectMapper).writeValueAsBytes(payloadCaptor.capture());
        FinalPackagePayload.VersionInfo versionInfo = payloadCaptor.getValue().getVersionInfo();
        assertThat(versionInfo).isNotNull();
        assertThat(versionInfo.getTenderDocumentAppVersion()).isEqualTo("35");
        assertThat(versionInfo.getTenderDocumentFormatVersion()).isEqualTo("2.1_35_SZCT");
        assertThat(versionInfo.getTenderDocumentUniqueCode()).isEqualTo("UNIQUE-CODE-001");
    }

    @Test
    void shouldGenerateOnlyOneFinalPackageForTenderScopedDocument() throws Exception {
        TenderDocument tenderDocument = buildDocument("邀请招标", TenderDocumentStatus.DRAFT);
        TenderDocumentFile purchaseFile = buildFile(TenderDocumentFileType.PURCHASE_SOURCE_PDF, TenderDocumentScopeType.TENDER, "T-01", 10L);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.TENDER, "T-01", 11L);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(buildCompletedSteps());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(purchaseFile, signedFile));
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(purchaseFile, signedFile);
        mockFinalPackagePipeline(tenderDocument, signedFile);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"packageCount\":1}");
        when(tenderDocumentGenerationGateway.saveFinalPackage(eq(tenderDocument), eq(TenderDocumentScopeType.TENDER), eq("T-01"), any(), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.TENDER, "T-01", 300L, "一标段包.HzctZbs"));
        when(tenderDocumentGenerationGateway.saveCompileInfoPdf(eq(tenderDocument), any()))
                .thenReturn(buildGeneratedFile(TenderDocumentFileType.COMPILE_INFO_PDF, TenderDocumentScopeType.PROJECT, null, 401L, "编制信息.pdf"));

        TenderDocumentGenerateResponse response = tenderDocumentGenerationService.generate(100L, buildContext());

        assertThat(response.getFinalPackageFile()).isNotNull();
        verify(tenderDocumentGenerationGateway, times(1))
                .saveFinalPackage(eq(tenderDocument), eq(TenderDocumentScopeType.TENDER), eq("T-01"), any(), any());
    }

    @Test
    void shouldRejectGenerateWhenRequiredStepsIncomplete() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        List<TenderDocumentStep> steps = buildCompletedSteps();
        steps.get(0).setStepStatus(TenderDocumentStepStatus.IN_PROGRESS.name());
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(steps);

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("步骤");
    }

    @Test
    void shouldRecordFailedPackageCallbackWithRetryCount() {
        TenderDocument tenderDocument = buildDocument("邀请招标", TenderDocumentStatus.COMPLETED);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any()))
                .thenReturn(buildFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.TENDER, "T-01", 200L));
        when(tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(eq(100L), eq(2), eq(TenderDocumentFileType.FINAL_PACKAGE_FILE.name()), eq("T-01")))
                .thenReturn(2);
        when(tenderDocumentCallbackGateway.callbackPackageFile(eq(tenderDocument), any(TenderDocumentFile.class), eq("T-01"), any(TenderDocumentUserContext.class)))
                .thenReturn(new TenderDocumentCallbackGatewayResult(false, "500", "回传失败"));

        TenderDocumentCallbackRequest request = new TenderDocumentCallbackRequest();
        request.setTenderId("T-01");
        TenderDocumentCallbackResponse response = tenderDocumentGenerationService.callbackPackageFile(100L, request, buildContext());

        assertThat(response.getRetryCount()).isEqualTo(2);
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getFileRole()).isEqualTo(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
        ArgumentCaptor<TenderDocumentCallback> callbackCaptor = ArgumentCaptor.forClass(TenderDocumentCallback.class);
        verify(tenderDocumentCallbackMapper).insert(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getCallbackStatus()).isEqualTo("FAIL");
        assertThat(callbackCaptor.getValue().getFileRole()).isEqualTo(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
    }

    @Test
    void shouldKeepTenderIdWhenProjectScopedCallback() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.COMPLETED);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any()))
                .thenReturn(buildFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.PROJECT, null, 200L));
        when(tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(
                eq(100L),
                eq(2),
                eq(TenderDocumentFileType.FINAL_PACKAGE_FILE.name()),
                eq("T-01")))
                .thenReturn(1);
        when(tenderDocumentCallbackGateway.callbackPackageFile(
                eq(tenderDocument),
                any(TenderDocumentFile.class),
                eq("T-01"),
                any(TenderDocumentUserContext.class)))
                .thenReturn(new TenderDocumentCallbackGatewayResult(true, "200", "回传成功"));

        TenderDocumentCallbackRequest request = new TenderDocumentCallbackRequest();
        request.setTenderId("T-01");
        TenderDocumentCallbackResponse response = tenderDocumentGenerationService.callbackPackageFile(100L, request, buildContext());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getTenderId()).isEqualTo("T-01");
        ArgumentCaptor<TenderDocumentCallback> callbackCaptor = ArgumentCaptor.forClass(TenderDocumentCallback.class);
        verify(tenderDocumentCallbackMapper).insert(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getTenderId()).isEqualTo("T-01");
    }

    @Test
    void shouldCallbackOnlySignedAndFinalPackageWhenCallingUnifiedCallback() {
        TenderDocument tenderDocument = buildDocument("邀请招标", TenderDocumentStatus.COMPLETED);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any()))
                .thenReturn(buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.TENDER, "T-01", 201L))
                .thenReturn(buildFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.TENDER, "T-01", 202L));
        when(tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(eq(100L), eq(2), eq(TenderDocumentFileType.SIGNED_PDF.name()), eq("T-01")))
                .thenReturn(1);
        when(tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(eq(100L), eq(2), eq(TenderDocumentFileType.FINAL_PACKAGE_FILE.name()), eq("T-01")))
                .thenReturn(1);
        when(tenderDocumentCallbackGateway.callbackSignedFile(eq(tenderDocument), any(TenderDocumentFile.class), eq("T-01"), any(TenderDocumentUserContext.class)))
                .thenReturn(new TenderDocumentCallbackGatewayResult(true, "200", "签章回传成功"));
        when(tenderDocumentCallbackGateway.callbackPackageFile(eq(tenderDocument), any(TenderDocumentFile.class), eq("T-01"), any(TenderDocumentUserContext.class)))
                .thenReturn(new TenderDocumentCallbackGatewayResult(true, "200", "数据包回传成功"));

        TenderDocumentCallbackRequest request = new TenderDocumentCallbackRequest();
        request.setTenderId("T-01");
        TenderDocumentUnifiedCallbackResponse response = tenderDocumentGenerationService.callbackAll(100L, request, buildContext());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getSignedFileResult()).isNotNull();
        assertThat(response.getPackageFileResult()).isNotNull();
        assertThat(response.getSignedFileResult().getFileRole()).isEqualTo(TenderDocumentFileType.SIGNED_PDF.name());
        assertThat(response.getPackageFileResult().getFileRole()).isEqualTo(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
        verify(tenderDocumentFileMapper, times(2)).selectOne(any());
        verify(tenderDocumentCallbackGateway, times(1))
                .callbackSignedFile(eq(tenderDocument), any(TenderDocumentFile.class), eq("T-01"), any(TenderDocumentUserContext.class));
        verify(tenderDocumentCallbackGateway, times(1))
                .callbackPackageFile(eq(tenderDocument), any(TenderDocumentFile.class), eq("T-01"), any(TenderDocumentUserContext.class));
    }

    @Test
    void shouldReturnPartialFailureWhenPackageCallbackThrowsException() {
        TenderDocument tenderDocument = buildDocument("邀请招标", TenderDocumentStatus.COMPLETED);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any()))
                .thenReturn(buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.TENDER, "T-01", 201L))
                .thenReturn(buildFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.TENDER, "T-01", 202L));
        when(tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(eq(100L), eq(2), eq(TenderDocumentFileType.SIGNED_PDF.name()), eq("T-01")))
                .thenReturn(1);
        when(tenderDocumentCallbackCounterMapper.selectCurrentRetryCount(eq(100L), eq(2), eq(TenderDocumentFileType.FINAL_PACKAGE_FILE.name()), eq("T-01")))
                .thenReturn(1);
        when(tenderDocumentCallbackGateway.callbackSignedFile(eq(tenderDocument), any(TenderDocumentFile.class), eq("T-01"), any(TenderDocumentUserContext.class)))
                .thenReturn(new TenderDocumentCallbackGatewayResult(true, "200", "签章回传成功"));
        when(tenderDocumentCallbackGateway.callbackPackageFile(eq(tenderDocument), any(TenderDocumentFile.class), eq("T-01"), any(TenderDocumentUserContext.class)))
                .thenThrow(new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_INTERACTION_FAILED.getCode(), "业务系统超时"));

        TenderDocumentCallbackRequest request = new TenderDocumentCallbackRequest();
        request.setTenderId("T-01");
        TenderDocumentUnifiedCallbackResponse response = tenderDocumentGenerationService.callbackAll(100L, request, buildContext());

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("回传失败:");
        assertThat(response.getMessage()).contains("FINAL_PACKAGE_FILE");
        assertThat(response.getMessage()).contains(String.valueOf(TenderDocumentErrorCode.BUSINESS_SYSTEM_INTERACTION_FAILED.getCode()));
        assertThat(response.getSignedFileResult().getSuccess()).isTrue();
        assertThat(response.getPackageFileResult().getSuccess()).isFalse();
        assertThat(response.getPackageFileResult().getResponseCode())
                .isEqualTo(String.valueOf(TenderDocumentErrorCode.BUSINESS_SYSTEM_INTERACTION_FAILED.getCode()));
        verify(tenderDocumentCallbackMapper, times(2)).insert(any(TenderDocumentCallback.class));
    }

    @Test
    void shouldRejectUnifiedCallbackWhenAnotherCallbackIsInProgress() {
        when(valueOperations.setIfAbsent(
                startsWith("tender-document:callback:lock:"),
                anyString(),
                anyLong(),
                eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        TenderDocumentCallbackRequest request = new TenderDocumentCallbackRequest();
        request.setTenderId("T-01");
        assertThatThrownBy(() -> tenderDocumentGenerationService.callbackAll(100L, request, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("回传正在进行");
        verify(tenderDocumentMapper, never()).selectById(anyLong());
    }

    @Test
    void shouldRejectGenerateWhenAlreadyGenerating() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.GENERATING);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(TenderDocumentErrorCode.GENERATION_IN_PROGRESS.getMessage());
        verify(tenderDocumentMapper, never()).update(any(TenderDocument.class), any());
    }

    @Test
    void shouldRejectGenerateWhenAlreadyCompleted() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.COMPLETED);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(TenderDocumentErrorCode.GENERATION_ALREADY_COMPLETED.getMessage());
        verify(tenderDocumentMapper, never()).update(any(TenderDocument.class), any());
    }

    @Test
    void shouldRejectGenerateWhenClaimStatusFailsBecauseAnotherRequestWon() {
        TenderDocument draftDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        TenderDocument generatingDocument = buildDocument("公开招标", TenderDocumentStatus.GENERATING);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(draftDocument, generatingDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(0);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(TenderDocumentErrorCode.GENERATION_IN_PROGRESS.getMessage());
    }

    @Test
    void shouldRejectGenerateWhenClaimStatusFailsAndLatestStatusIsCompleted() {
        TenderDocument draftDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        TenderDocument completedDocument = buildDocument("公开招标", TenderDocumentStatus.COMPLETED);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(draftDocument, completedDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(0);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(TenderDocumentErrorCode.GENERATION_ALREADY_COMPLETED.getMessage());
    }

    @Test
    void shouldRollbackToDraftWhenGenerateFails() throws Exception {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(buildCompletedSteps());
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.PROJECT, null, 11L);
        when(tenderDocumentFileMapper.selectOne(any()))
                .thenReturn(buildFile(TenderDocumentFileType.PURCHASE_SOURCE_PDF, TenderDocumentScopeType.PROJECT, null, 10L))
                .thenReturn(signedFile);
        mockFinalPackagePipeline(tenderDocument, signedFile);
        when(tenderDocumentGenerationGateway.saveFinalPackage(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(TenderDocumentErrorCode.FILE_BINDING_NOT_FOUND.getCode(), "boom"));

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class);
        ArgumentCaptor<TenderDocument> captor = ArgumentCaptor.forClass(TenderDocument.class);
        verify(tenderDocumentMapper, times(2)).update(captor.capture(), any());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(TenderDocumentStatus.GENERATING.name());
        assertThat(captor.getAllValues().get(1).getStatus())
                .isEqualTo(TenderDocumentStatus.DRAFT.name());
        verify(tenderDocumentGenerationRecordService).createProcessingRecord(eq(tenderDocument), any(TenderDocumentUserContext.class));
        verify(tenderDocumentGenerationRecordService).markFail(
                eq(900L),
                eq(String.valueOf(TenderDocumentErrorCode.FILE_BINDING_NOT_FOUND.getCode())),
                eq("boom"),
                any(Date.class)
        );
    }

    @Test
    void shouldRejectGenerateWhenSignedPdfMissing() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(buildCompletedSteps());
        when(tenderDocumentFileMapper.selectOne(any()))
                .thenReturn(buildFile(TenderDocumentFileType.PURCHASE_SOURCE_PDF, TenderDocumentScopeType.PROJECT, null, 10L))
                .thenReturn(null);

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(TenderDocumentFileType.SIGNED_PDF.name());
    }

    @Test
    void shouldRejectGenerateWhenCaKeysInfoMissing() throws Exception {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        TenderDocumentFile purchaseFile = buildFile(TenderDocumentFileType.PURCHASE_SOURCE_PDF, TenderDocumentScopeType.PROJECT, null, 10L);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.PROJECT, null, 11L);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(tenderDocumentMapper.update(any(TenderDocument.class), any())).thenReturn(1);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectList(any())).thenReturn(buildCompletedSteps());
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(purchaseFile, signedFile);
        when(finalPackageDataAssembler.assemble(tenderDocument)).thenReturn(new FinalPackagePayload());
        when(signedPdfContentLoader.load(signedFile)).thenReturn(new FinalPackagePayload.TenderDocumentSignPdf());
        when(businessSystemRemoteClient.queryCaKeysInfo(any(), any(), any(), any())).thenReturn(new CaKeysInfoResponse());

        assertThatThrownBy(() -> tenderDocumentGenerationService.generate(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("开标时有效的caKeysInfo");
        verify(tenderDocumentGenerationGateway, never()).saveFinalPackage(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectGeneratePageWhenMultiplePackageFiles() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.PROJECT, null, 10L);
        TenderDocumentFile packageFile1 = buildFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.PROJECT, null, 20L);
        TenderDocumentFile packageFile2 = buildFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.PROJECT, null, 21L);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(signedFile, packageFile1, packageFile2));

        assertThatThrownBy(() -> tenderDocumentGenerationService.getGeneratePage(100L, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("多份数据包");
    }

    @Test
    void shouldGetGeneratePageWithLatestGenerationRecordAndCompileCompleteTime() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.COMPLETED);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.PROJECT, null, 10L);
        TenderDocumentFile finalPackageFile = buildFile(TenderDocumentFileType.FINAL_PACKAGE_FILE, TenderDocumentScopeType.PROJECT, null, 20L);
        TenderDocumentFile compileInfoFile = buildFile(TenderDocumentFileType.COMPILE_INFO_PDF, TenderDocumentScopeType.PROJECT, null, 30L);
        TenderDocumentGenerationRecord latestRecord = buildGenerationRecord(901L);
        latestRecord.setVersionNo(2);
        latestRecord.setGenerateStatus("SUCCESS");
        latestRecord.setStartTime(new Date(1765528083000L));
        latestRecord.setEndTime(new Date(1765528084000L));
        latestRecord.setDurationMs(1000L);
        latestRecord.setOperatorId("u-1");
        latestRecord.setOperatorName("测试用户");
        TenderDocumentVersion version = new TenderDocumentVersion();
        version.setVersionNo(2);
        version.setGenerateTime(new Date(1765528085000L));

        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(signedFile, finalPackageFile, compileInfoFile));
        when(tenderDocumentCallbackMapper.selectList(any())).thenReturn(List.of());
        when(tenderDocumentVersionMapper.selectOne(any())).thenReturn(version);
        when(tenderDocumentGenerationRecordService.findLatestByTenderDocumentId(100L)).thenReturn(latestRecord);

        var response = tenderDocumentGenerationService.getGeneratePage(100L, buildContext());

        assertThat(response.getCompileCompleteTime()).isEqualTo(version.getGenerateTime());
        assertThat(response.getLatestGenerateRecord()).isNotNull();
        assertThat(response.getLatestGenerateRecord().getId()).isEqualTo(901L);
        assertThat(response.getLatestGenerateRecord().getGenerateStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldHideOldCompileInfoAndRecordAfterRecompileBeforeNewGeneration() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.DRAFT);
        tenderDocument.setVersionNo(3);
        TenderDocumentFile signedFile = buildFile(TenderDocumentFileType.SIGNED_PDF, TenderDocumentScopeType.PROJECT, null, 10L);
        TenderDocumentGenerationRecord latestRecord = buildGenerationRecord(902L);
        latestRecord.setVersionNo(2);
        latestRecord.setGenerateStatus("SUCCESS");
        TenderDocumentVersion oldVersion = new TenderDocumentVersion();
        oldVersion.setVersionNo(2);
        oldVersion.setGenerateTime(new Date(1765528085000L));

        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(signedFile));
        when(tenderDocumentCallbackMapper.selectList(any())).thenReturn(List.of());
        when(tenderDocumentVersionMapper.selectOne(any())).thenReturn(oldVersion);
        when(tenderDocumentGenerationRecordService.findLatestByTenderDocumentId(100L)).thenReturn(latestRecord);

        var response = tenderDocumentGenerationService.getGeneratePage(100L, buildContext());

        assertThat(response.getCompileInfoFile()).isNull();
        assertThat(response.getCompileCompleteTime()).isNull();
        assertThat(response.getLatestGenerateRecord()).isNull();
    }

    @Test
    void shouldListGenerationRecordsForGeneratePageQuery() {
        TenderDocument tenderDocument = buildDocument("公开招标", TenderDocumentStatus.COMPLETED);
        TenderDocumentGenerationRecord r1 = buildGenerationRecord(902L);
        r1.setGenerateStatus("FAIL");
        r1.setErrorCode("6012");
        r1.setErrorMessage("缺少可用的SIGNED_PDF文件");
        TenderDocumentGenerationRecord r2 = buildGenerationRecord(903L);
        r2.setGenerateStatus("SUCCESS");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentGenerationRecordService.listByTenderDocumentId(100L)).thenReturn(List.of(r2, r1));

        List<TenderDocumentGenerationRecordView> records = tenderDocumentGenerationService.listGenerateRecords(100L, buildContext());

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getId()).isEqualTo(903L);
        assertThat(records.get(1).getGenerateStatus()).isEqualTo("FAIL");
        assertThat(records.get(1).getErrorCode()).isEqualTo("6012");
    }

    private TenderDocument buildDocument(String purchaseMethod, TenderDocumentStatus status) {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setProjectCode("XM-001");
        tenderDocument.setProjectName("项目一");
        tenderDocument.setPurchaseMethod(purchaseMethod);
        tenderDocument.setCompileScope(purchaseMethod.contains("邀请")
                ? TenderDocumentScopeType.TENDER.name()
                : TenderDocumentScopeType.PROJECT.name());
        tenderDocument.setTenderId(purchaseMethod.contains("邀请") ? "T-01" : null);
        tenderDocument.setStatus(status.name());
        tenderDocument.setVersionNo(2);
        tenderDocument.setCurrentStepCode(TenderDocumentStepCode.GENERATE_PACKAGE.name());
        return tenderDocument;
    }

    private void mockFinalPackagePipeline(TenderDocument tenderDocument, TenderDocumentFile signedFile) throws Exception {
        when(finalPackageDataAssembler.assemble(tenderDocument)).thenReturn(new FinalPackagePayload());
        when(signedPdfContentLoader.load(signedFile)).thenReturn(new FinalPackagePayload.TenderDocumentSignPdf());
        CaKeysInfoResponse.CaKeyInfo caKeyInfo = new CaKeysInfoResponse.CaKeyInfo();
        caKeyInfo.setEncryptOrder(1);
        caKeyInfo.setUserId("u-1");
        caKeyInfo.setCaId("ca-id");
        caKeyInfo.setCaNo("ca-no");
        caKeyInfo.setPublicKey("public-key");
        CaKeysInfoResponse caKeysInfoResponse = new CaKeysInfoResponse();
        caKeysInfoResponse.setCaKeysInfo(List.of(caKeyInfo));
        when(businessSystemRemoteClient.queryCaKeysInfo(any(), any(), any(), any())).thenReturn(caKeysInfoResponse);
        when(objectMapper.writeValueAsBytes(any())).thenReturn("plain".getBytes());
        when(finalPackageEncryptor.encrypt(any())).thenReturn("encrypted".getBytes());
        lenient().when(compileInfoPdfGenerator.generate(eq(tenderDocument), any(), any(), any(), any())).thenReturn("compile-info".getBytes());
    }

    private List<TenderDocumentStep> buildCompletedSteps() {
        return List.of(
                buildStep(TenderDocumentStepCode.BASIC_INFO, TenderDocumentStepStatus.COMPLETED),
                buildStep(TenderDocumentStepCode.PURCHASE_FILE, TenderDocumentStepStatus.COMPLETED),
                buildStep(TenderDocumentStepCode.BID_RECORD, TenderDocumentStepStatus.COMPLETED),
                buildStep(TenderDocumentStepCode.EVALUATION_RULE, TenderDocumentStepStatus.COMPLETED),
                buildStep(TenderDocumentStepCode.CHECK_ITEMS, TenderDocumentStepStatus.COMPLETED),
                buildStep(TenderDocumentStepCode.GENERATE_PACKAGE, TenderDocumentStepStatus.IN_PROGRESS)
        );
    }

    private TenderDocumentStep buildStep(TenderDocumentStepCode stepCode, TenderDocumentStepStatus status) {
        TenderDocumentStep step = new TenderDocumentStep();
        step.setId((long) stepCode.getStepOrder());
        step.setTenderDocumentId(100L);
        step.setStepCode(stepCode.name());
        step.setStepOrder(stepCode.getStepOrder());
        step.setStepStatus(status.name());
        return step;
    }

    private TenderDocumentFile buildFile(TenderDocumentFileType fileRole, TenderDocumentScopeType scopeType, String tenderId, Long fileId) {
        TenderDocumentFile file = new TenderDocumentFile();
        file.setId(fileId);
        file.setTenderDocumentId(100L);
        file.setProjectId("P-100");
        file.setTenderId(tenderId);
        file.setFileRole(fileRole.name());
        file.setScopeType(scopeType.name());
        file.setFileId(fileId);
        file.setFileName(fileRole.name() + ".pdf");
        file.setActiveFlag(1);
        file.setCreatedTime(new Date());
        return file;
    }

    private TenderDocumentGeneratedFile buildGeneratedFile(TenderDocumentFileType fileRole, TenderDocumentScopeType scopeType,
                                                           String tenderId, Long fileId, String fileName) {
        TenderDocumentGeneratedFile file = new TenderDocumentGeneratedFile();
        file.setFileRole(fileRole.name());
        file.setScopeType(scopeType.name());
        file.setTenderId(tenderId);
        file.setFileId(fileId);
        file.setFileName(fileName);
        file.setFileSize(1024L);
        file.setContentType("application/octet-stream");
        file.setFileSha256("sha256-" + fileId);
        return file;
    }

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("app-a");
        context.setUserId("u-1");
        context.setUserName("测试用户");
        context.setEnterpriseCode("913301");
        return context;
    }

    private TenderDocumentGenerationRecord buildGenerationRecord(Long recordId) {
        TenderDocumentGenerationRecord record = new TenderDocumentGenerationRecord();
        record.setId(recordId);
        record.setGenerateStatus("PROCESSING");
        record.setStartTime(new Date());
        return record;
    }
}
