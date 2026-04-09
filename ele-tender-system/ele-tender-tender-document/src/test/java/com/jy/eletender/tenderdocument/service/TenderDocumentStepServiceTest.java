package com.jy.eletender.tenderdocument.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.crypto.digest.DigestUtil;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.entity.TenderDocumentSnapshot;
import com.jy.eletender.tenderdocument.entity.TenderDocumentStep;
import com.jy.eletender.tenderdocument.entity.TenderRuleHeader;
import com.jy.eletender.tenderdocument.entity.TenderRuleScoreConfig;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepStatus;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentSnapshotMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentStepMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleScoreConfigMapper;
import com.jy.eletender.tenderdocument.service.impl.TenderDocumentStepServiceImpl;
import com.jy.eletender.tenderdocument.support.DefaultTenderDocumentSyncGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentBasicInfoSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentBidRecordSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleValidator;
import com.jy.eletender.tenderdocument.support.TenderDocumentSyncGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderDocumentStepServiceTest {

    @Mock
    private TenderDocumentMapper tenderDocumentMapper;

    @Mock
    private TenderDocumentStepMapper tenderDocumentStepMapper;

    @Mock
    private TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper;

    @Mock
    private TenderDocumentFileMapper tenderDocumentFileMapper;

    @Mock
    private TenderRuleHeaderMapper tenderRuleHeaderMapper;

    @Mock
    private TenderRuleScoreConfigMapper tenderRuleScoreConfigMapper;

    @Mock
    private TenderDocumentSyncGateway tenderDocumentSyncGateway;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private IProjectLockService projectLockService;

    @Spy
    private TenderDocumentRuleValidator tenderDocumentRuleValidator = new TenderDocumentRuleValidator();

    @InjectMocks
    private TenderDocumentStepServiceImpl tenderDocumentStepService;

    @Test
    void shouldReturnCheckItemsPage() {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());

        assertThat(tenderDocumentStepService.getCheckItems(100L, buildContext()).getItemList()).isNotEmpty();
    }

    @Test
    void shouldSyncBasicInfoAndUpdateLatestSyncTime() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoSyncResult());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"projectId\":\"P-100\"}");

        tenderDocumentStepService.syncBasicInfo(100L, buildContext());

        verify(tenderDocumentSnapshotMapper).insert(any(TenderDocumentSnapshot.class));
        verify(tenderDocumentMapper).updateById(any(TenderDocument.class));
    }

    @Test
    void shouldPersistTenderAmountIntoBasicInfoSnapshotPayload() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        tenderDocument.setTenderId("T-01");
        TenderDocumentBasicInfoSyncResult payload = new DefaultTenderDocumentSyncGateway().syncBasicInfo(tenderDocument, buildContext());
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(payload);
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            TenderDocumentBasicInfoSyncResult snapshotPayload = invocation.getArgument(0);
            assertThat(snapshotPayload.getTenderList()).hasSize(1);
            assertThat(snapshotPayload.getTenderList().get(0))
                    .containsEntry("tenderAmount", new java.math.BigDecimal("1000000.00"));
            return "{\"projectId\":\"P-100\"}";
        }).when(objectMapper).writeValueAsString(any());

        tenderDocumentStepService.syncBasicInfo(100L, buildContext());

        verify(tenderDocumentSnapshotMapper).insert(any(TenderDocumentSnapshot.class));
    }

    @Test
    void shouldWriteSnapshotJsonSha256AsSourceHashWhenInsertSnapshot() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentBasicInfoSyncResult payload = buildBasicInfoSyncResult();
        payload.setProjectType(InteractionProjectType.PUBLIC);
        String snapshotJson = "{\"projectId\":\"P-100\"}";
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(payload);
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn(snapshotJson);
        doAnswer(invocation -> {
            TenderDocumentSnapshot snapshot = invocation.getArgument(0);
            assertThat(snapshot.getSnapshotJson()).isEqualTo(snapshotJson);
            assertThat(snapshot.getSourceHash()).isEqualTo(DigestUtil.sha256Hex(snapshotJson));
            return 1;
        }).when(tenderDocumentSnapshotMapper).insert(any(TenderDocumentSnapshot.class));

        tenderDocumentStepService.syncBasicInfo(100L, buildContext());

        verify(tenderDocumentSnapshotMapper).insert(any(TenderDocumentSnapshot.class));
    }

    @Test
    void shouldRefreshSourceHashWhenUpdateSnapshot() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentBidRecordSyncResult payload = new TenderDocumentBidRecordSyncResult();
        payload.setProjectInfo(Map.of("projectId", "P-100"));
        String snapshotJson = "{\"projectId\":\"P-100\",\"bidRecord\":{}}";
        TenderDocumentSnapshot existingSnapshot = new TenderDocumentSnapshot();
        existingSnapshot.setId(10L);
        existingSnapshot.setTenderDocumentId(100L);
        existingSnapshot.setStepCode(TenderDocumentStepCode.BID_RECORD.name());
        existingSnapshot.setSnapshotJson("{\"old\":true}");
        existingSnapshot.setSourceHash("old-hash");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBidRecord(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(payload);
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(existingSnapshot);
        when(objectMapper.writeValueAsString(any())).thenReturn(snapshotJson);
        when(objectMapper.readValue(snapshotJson, Map.class)).thenReturn(Map.of(
                "projectInfo", Map.of("projectId", "P-100"),
                "tenderList", List.of(),
                "bidRecord", Map.of()
        ));
        doAnswer(invocation -> {
            TenderDocumentSnapshot snapshot = invocation.getArgument(0);
            assertThat(snapshot.getId()).isEqualTo(10L);
            assertThat(snapshot.getSnapshotJson()).isEqualTo(snapshotJson);
            assertThat(snapshot.getSourceHash()).isEqualTo(DigestUtil.sha256Hex(snapshotJson));
            return 1;
        }).when(tenderDocumentSnapshotMapper).updateById(any(TenderDocumentSnapshot.class));

        tenderDocumentStepService.syncBidRecord(100L, buildContext());

        verify(tenderDocumentSnapshotMapper).updateById(any(TenderDocumentSnapshot.class));
    }

    @Test
    void shouldResolveCompileScopeFromProjectTypeWhenSyncBasicInfo() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentBasicInfoSyncResult payload = buildBasicInfoSyncResult();
        payload.setCompileScope(null);
        payload.setProjectType(InteractionProjectType.INVITE);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(payload);
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"projectId\":\"P-100\"}");
        doAnswer(invocation -> {
            TenderDocument updated = invocation.getArgument(0);
            assertThat(updated.getCompileScope()).isEqualTo(TenderDocumentScopeType.TENDER.name());
            assertThat(updated.getIndexOf()).isEqualTo(3);
            return 1;
        }).when(tenderDocumentMapper).updateById(any(TenderDocument.class));

        tenderDocumentStepService.syncBasicInfo(100L, buildContext());

        verify(tenderDocumentMapper).updateById(any(TenderDocument.class));
    }

    @Test
    void shouldEmbedBidRecordIntoEachTenderItemWhenLoadingBidRecordPage() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        Date sourceSyncTime = new Date();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode(TenderDocumentStepCode.BID_RECORD.name());
        snapshot.setSnapshotJson("{\"tenderList\":[],\"bidRecord\":{}}");
        snapshot.setSourceSyncTime(sourceSyncTime);
        Map<String, Object> bidRecord = Map.of("schemeContent", "[{\"sign\":\"remark\",\"description\":\"备注\"}]");
        Map<String, Object> payload = Map.of(
                "projectInfo", Map.of("projectId", "P-100"),
                "tenderList", List.of(
                        Map.of("tenderId", "T-02", "tenderName", "二标段", "indexOf", 2),
                        Map.of("tenderId", "T-01", "tenderName", "一标段", "indexOf", 1)
                ),
                "bidRecord", bidRecord
        );
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(snapshot);
        when(objectMapper.readValue(snapshot.getSnapshotJson(), Map.class)).thenReturn(payload);

        var response = tenderDocumentStepService.getBidRecordPage(100L, buildContext());

        assertThat(response.getTenderList()).hasSize(2);
        assertThat(response.getTenderList().get(0)).containsEntry("tenderId", "T-01");
        assertThat(response.getTenderList().get(0)).containsEntry("indexOfDesc", "标段一");
        assertThat(response.getTenderList().get(0)).containsEntry("bidRecord", bidRecord);
        assertThat(response.getTenderList().get(1)).containsEntry("tenderId", "T-02");
        assertThat(response.getTenderList().get(1)).containsEntry("indexOfDesc", "标段二");
        assertThat(response.getLastSyncTime()).isEqualTo(sourceSyncTime);
    }

    @Test
    void shouldSortTenderListByIndexOfAndAppendIndexOfDescWhenLoadingBasicInfoPage() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode(TenderDocumentStepCode.BASIC_INFO.name());
        snapshot.setSnapshotJson("{\"tenderList\":[]}");
        Map<String, Object> payload = Map.of(
                "projectInfo", Map.of("projectId", "P-100"),
                "tenderList", List.of(
                        Map.of("tenderId", "T-02", "tenderName", "二标段", "indexOf", "2"),
                        Map.of("tenderId", "T-01", "tenderName", "一标段", "indexOf", 1)
                )
        );
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(snapshot);
        when(objectMapper.readValue(snapshot.getSnapshotJson(), Map.class)).thenReturn(payload);

        var response = tenderDocumentStepService.getBasicInfoPage(100L, buildContext());

        assertThat(response.getTenderList()).hasSize(2);
        assertThat(response.getTenderList().get(0)).containsEntry("tenderId", "T-01");
        assertThat(response.getTenderList().get(0)).containsEntry("indexOfDesc", "标段一");
        assertThat(response.getTenderList().get(1)).containsEntry("tenderId", "T-02");
        assertThat(response.getTenderList().get(1)).containsEntry("indexOfDesc", "标段二");
    }

    @Test
    void shouldCompleteCurrentStepAndAdvanceNextStep() {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentStep currentStep = buildStep(TenderDocumentStepCode.BASIC_INFO, TenderDocumentStepStatus.IN_PROGRESS);
        TenderDocumentStep nextStep = buildStep(TenderDocumentStepCode.PURCHASE_FILE, TenderDocumentStepStatus.PENDING);
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode(TenderDocumentStepCode.BASIC_INFO.name());
        snapshot.setSnapshotJson("{\"projectId\":\"P-100\"}");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(currentStep, nextStep);
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(snapshot);

        tenderDocumentStepService.completeStep(100L, TenderDocumentStepCode.BASIC_INFO, buildContext());

        verify(tenderDocumentStepMapper, org.mockito.Mockito.times(2)).updateById(any(TenderDocumentStep.class));
        verify(tenderDocumentMapper).updateById(any(TenderDocument.class));
    }

    @Test
    void shouldRejectStepCompletionWhenBasicInfoNotSynced() {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentStep currentStep = buildStep(TenderDocumentStepCode.BASIC_INFO, TenderDocumentStepStatus.IN_PROGRESS);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(currentStep);
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> tenderDocumentStepService.completeStep(100L, TenderDocumentStepCode.BASIC_INFO, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("基本信息");
    }

    @Test
    void shouldRejectStepCompletionWhenEvaluationRuleNotConfigured() {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentStep currentStep = buildStep(TenderDocumentStepCode.EVALUATION_RULE, TenderDocumentStepStatus.IN_PROGRESS);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(currentStep);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> tenderDocumentStepService.completeStep(100L, TenderDocumentStepCode.EVALUATION_RULE, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("评审规则");
    }

    @Test
    void shouldRejectStepCompletionWhenEvaluationRuleCrossCategoryInvalid() {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentStep currentStep = buildStep(TenderDocumentStepCode.EVALUATION_RULE, TenderDocumentStepStatus.IN_PROGRESS);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(currentStep);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of(
                buildHeader("QUALIFICATION", "PASS", null, null),
                buildHeader("CONFORMITY", "PASS", null, null),
                buildHeader("CREDIT", "SCORE", 40, 100),
                buildHeader("TECHNICAL", "SCORE", 30, 100),
                buildHeader("BUSINESS", "SCORE", 20, 100)
        ));
        TenderRuleScoreConfig scoreConfig = new TenderRuleScoreConfig();
        scoreConfig.setTenderDocumentId(100L);
        scoreConfig.setScoreType("ACTUAL");
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(scoreConfig);

        assertThatThrownBy(() -> tenderDocumentStepService.completeStep(100L, TenderDocumentStepCode.EVALUATION_RULE, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("总分之和必须等于100");
    }

    @Test
    void shouldRejectStepCompletionWhenComprehensiveScoreTypeMissing() {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentStep currentStep = buildStep(TenderDocumentStepCode.EVALUATION_RULE, TenderDocumentStepStatus.IN_PROGRESS);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(currentStep);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of(
                buildHeader("QUALIFICATION", "PASS", null, null),
                buildHeader("CONFORMITY", "PASS", null, null),
                buildHeader("CREDIT", "SCORE", 40, 100),
                buildHeader("TECHNICAL", "SCORE", 30, 30),
                buildHeader("BUSINESS", "SCORE", 30, 70)
        ));
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> tenderDocumentStepService.completeStep(100L, TenderDocumentStepCode.EVALUATION_RULE, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("scoreType");
    }

    // ========== check-items tests ==========

    @Test
    void checkItems_basicInfo_passed_whenHashMatches() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        TenderDocumentBasicInfoSyncResult latestResult = buildBasicInfoSyncResult();
        String snapshotJson = "{\"projectId\":\"P-100\"}";
        String expectedHash = DigestUtil.sha256Hex(snapshotJson);
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode(TenderDocumentStepCode.BASIC_INFO.name());
        snapshot.setSnapshotJson(snapshotJson);
        snapshot.setSourceHash(expectedHash);
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(snapshot, (TenderDocumentSnapshot) null, (TenderDocumentSnapshot) null);
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(latestResult);
        when(objectMapper.writeValueAsString(latestResult)).thenReturn(snapshotJson);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList()).hasSize(4);
        assertThat(response.getItemList().get(0).getItemCode()).isEqualTo("BASIC_INFO");
        assertThat(response.getItemList().get(0).getPassed()).isTrue();
        assertThat(response.getItemList().get(0).getMessage()).contains("已是最新");
    }

    @Test
    void checkItems_basicInfo_failed_whenHashDiffers() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode(TenderDocumentStepCode.BASIC_INFO.name());
        snapshot.setSnapshotJson("{\"old\":true}");
        snapshot.setSourceHash("old-hash");
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(snapshot, (TenderDocumentSnapshot) null, (TenderDocumentSnapshot) null);
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBasicInfoSyncResult());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"new\":true}");
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(0).getItemCode()).isEqualTo("BASIC_INFO");
        assertThat(response.getItemList().get(0).getPassed()).isFalse();
        assertThat(response.getItemList().get(0).getMessage()).contains("已更新");
    }

    @Test
    void checkItems_basicInfo_failed_whenSnapshotMissing() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(0).getItemCode()).isEqualTo("BASIC_INFO");
        assertThat(response.getItemList().get(0).getPassed()).isFalse();
        assertThat(response.getItemList().get(0).getMessage()).contains("尚未同步");
    }

    @Test
    void checkItems_basicInfo_failed_whenSyncThrowsException() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode(TenderDocumentStepCode.BASIC_INFO.name());
        snapshot.setSnapshotJson("{\"exists\":true}");
        snapshot.setSourceHash("some-hash");
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(snapshot, (TenderDocumentSnapshot) null, (TenderDocumentSnapshot) null);
        when(tenderDocumentSyncGateway.syncBasicInfo(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenThrow(new RuntimeException("连接超时"));
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(0).getItemCode()).isEqualTo("BASIC_INFO");
        assertThat(response.getItemList().get(0).getPassed()).isFalse();
        assertThat(response.getItemList().get(0).getMessage()).contains("同步检查失败");
    }

    @Test
    void checkItems_purchaseFile_passed_whenSignedPdfExists() {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        TenderDocumentFile signedFile = new TenderDocumentFile();
        signedFile.setTenderDocumentId(100L);
        signedFile.setFileRole("SIGNED_PDF");
        signedFile.setActiveFlag(1);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(signedFile);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(1).getItemCode()).isEqualTo("PURCHASE_FILE");
        assertThat(response.getItemList().get(1).getPassed()).isTrue();
        assertThat(response.getItemList().get(1).getMessage()).contains("签章文件已上传");
    }

    @Test
    void checkItems_purchaseFile_failed_whenSignedPdfMissing() {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(1).getItemCode()).isEqualTo("PURCHASE_FILE");
        assertThat(response.getItemList().get(1).getPassed()).isFalse();
        assertThat(response.getItemList().get(1).getMessage()).contains("签章文件尚未上传");
    }

    @Test
    void checkItems_bidRecord_passed_whenHashMatches() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentBidRecordSyncResult latestResult = buildBidRecordSyncResult();
        String snapshotJson = "{\"bidRecord\":{}}";
        String expectedHash = DigestUtil.sha256Hex(snapshotJson);
        TenderDocumentSnapshot bidSnapshot = new TenderDocumentSnapshot();
        bidSnapshot.setTenderDocumentId(100L);
        bidSnapshot.setStepCode(TenderDocumentStepCode.BID_RECORD.name());
        bidSnapshot.setSnapshotJson(snapshotJson);
        bidSnapshot.setSourceHash(expectedHash);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn((TenderDocumentSnapshot) null, bidSnapshot);
        when(tenderDocumentSyncGateway.syncBidRecord(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(latestResult);
        when(objectMapper.writeValueAsString(latestResult)).thenReturn(snapshotJson);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(2).getItemCode()).isEqualTo("BID_RECORD");
        assertThat(response.getItemList().get(2).getPassed()).isTrue();
        assertThat(response.getItemList().get(2).getMessage()).contains("已是最新");
    }

    @Test
    void checkItems_bidRecord_failed_whenHashDiffers() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentSnapshot bidSnapshot = new TenderDocumentSnapshot();
        bidSnapshot.setTenderDocumentId(100L);
        bidSnapshot.setStepCode(TenderDocumentStepCode.BID_RECORD.name());
        bidSnapshot.setSnapshotJson("{\"old\":true}");
        bidSnapshot.setSourceHash("old-hash");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn((TenderDocumentSnapshot) null, bidSnapshot);
        when(tenderDocumentSyncGateway.syncBidRecord(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenReturn(buildBidRecordSyncResult());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"new\":true}");
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(2).getItemCode()).isEqualTo("BID_RECORD");
        assertThat(response.getItemList().get(2).getPassed()).isFalse();
        assertThat(response.getItemList().get(2).getMessage()).contains("已更新");
    }

    @Test
    void checkItems_bidRecord_failed_whenSyncThrowsException() throws Exception {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentSnapshot bidSnapshot = new TenderDocumentSnapshot();
        bidSnapshot.setTenderDocumentId(100L);
        bidSnapshot.setStepCode(TenderDocumentStepCode.BID_RECORD.name());
        bidSnapshot.setSnapshotJson("{\"exists\":true}");
        bidSnapshot.setSourceHash("some-hash");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn((TenderDocumentSnapshot) null, bidSnapshot);
        when(tenderDocumentSyncGateway.syncBidRecord(any(TenderDocument.class), any(TenderDocumentUserContext.class)))
                .thenThrow(new RuntimeException("连接超时"));
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(2).getItemCode()).isEqualTo("BID_RECORD");
        assertThat(response.getItemList().get(2).getPassed()).isFalse();
        assertThat(response.getItemList().get(2).getMessage()).contains("同步检查失败");
    }

    @Test
    void checkItems_evaluationRule_passed_whenValidationSucceeds() {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of(
                buildHeader("QUALIFICATION", "PASS", null, null),
                buildHeader("CONFORMITY", "PASS", null, null),
                buildHeader("CREDIT", "SCORE", 40, 100),
                buildHeader("TECHNICAL", "SCORE", 30, 100),
                buildHeader("BUSINESS", "SCORE", 30, 100)
        ));
        TenderRuleScoreConfig scoreConfig = new TenderRuleScoreConfig();
        scoreConfig.setTenderDocumentId(100L);
        scoreConfig.setScoreType("ACTUAL");
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(scoreConfig);

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(3).getItemCode()).isEqualTo("EVALUATION_RULE");
        assertThat(response.getItemList().get(3).getPassed()).isTrue();
        assertThat(response.getItemList().get(3).getMessage()).contains("校验通过");
    }

    @Test
    void checkItems_evaluationRule_failed_whenRuleNotConfigured() {
        TenderDocument tenderDocument = buildDocument();
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(null);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        var response = tenderDocumentStepService.getCheckItems(100L, buildContext());

        assertThat(response.getItemList().get(3).getItemCode()).isEqualTo("EVALUATION_RULE");
        assertThat(response.getItemList().get(3).getPassed()).isFalse();
        assertThat(response.getItemList().get(3).getMessage()).contains("评审规则");
    }

    @Test
    void shouldRejectPurchaseFileCompletionWhenSignedFileMissing() {
        TenderDocument tenderDocument = buildDocument();
        TenderDocumentStep currentStep = buildStep(TenderDocumentStepCode.PURCHASE_FILE, TenderDocumentStepStatus.IN_PROGRESS);
        TenderDocumentFile purchaseFile = new TenderDocumentFile();
        purchaseFile.setTenderDocumentId(100L);
        purchaseFile.setScopeType(TenderDocumentScopeType.PROJECT.name());
        purchaseFile.setFileRole("PURCHASE_SOURCE_PDF");
        purchaseFile.setActiveFlag(1);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentStepMapper.selectOne(any())).thenReturn(currentStep);
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(purchaseFile, null);

        assertThatThrownBy(() -> tenderDocumentStepService.completeStep(100L, TenderDocumentStepCode.PURCHASE_FILE, buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("签章文件");
    }

    private TenderDocument buildDocument() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setStatus(TenderDocumentStatus.DRAFT.name());
        tenderDocument.setCurrentStepCode(TenderDocumentStepCode.BASIC_INFO.name());
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        tenderDocument.setPurchaseMethod("公开招标");
        tenderDocument.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        tenderDocument.setTenderId("T-01");
        return tenderDocument;
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

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("app-a");
        context.setUserId("u-1");
        context.setUserName("测试用户");
        context.setEnterpriseCode("913301");
        return context;
    }

    private TenderRuleHeader buildHeader(String category, String reviewMode, Integer totalScore, Integer weightRate) {
        TenderRuleHeader header = new TenderRuleHeader();
        header.setTenderDocumentId(100L);
        header.setTenderId("T-01");
        header.setNodeCategory(category);
        header.setReviewMode(reviewMode);
        if (totalScore != null && totalScore > 0) {
            header.setTotalScore(java.math.BigDecimal.valueOf(totalScore));
        }
        if (weightRate != null && weightRate > 0) {
            header.setWeightRate(java.math.BigDecimal.valueOf(weightRate));
        }
        return header;
    }

    private TenderDocumentBasicInfoSyncResult buildBasicInfoSyncResult() {
        TenderDocumentBasicInfoSyncResult result = new TenderDocumentBasicInfoSyncResult();
        result.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        result.setProjectType(InteractionProjectType.PUBLIC);
        result.setProjectCode("PJT-001");
        result.setProjectName("项目一");
        result.setPurchaseMethod("公开招标");
        result.setEvalMethod(InteractionEvalMethod.COMPREHENSIVE_SCORE);
        result.setTenderList(List.of(
                Map.of("tenderId", "T-01", "indexOf", 3),
                Map.of("tenderId", "T-02", "indexOf", 4)
        ));
        return result;
    }

    private TenderDocumentBidRecordSyncResult buildBidRecordSyncResult() {
        TenderDocumentBidRecordSyncResult result = new TenderDocumentBidRecordSyncResult();
        result.setProjectInfo(Map.of("projectId", "P-100"));
        result.setTenderList(List.of(Map.of("tenderId", "T-01")));
        result.setBidRecord(Map.of("field", "value"));
        return result;
    }
}
