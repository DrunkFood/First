package com.jy.eletender.tenderdocument.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleTreeNode;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentScoreTypeSaveRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEvaluationRulesPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentRuleResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentScoreTypeResponse;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentSnapshot;
import com.jy.eletender.tenderdocument.entity.TenderRuleScoreConfig;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentSnapshotMapper;
import com.jy.eletender.tenderdocument.entity.TenderRuleHeader;
import com.jy.eletender.tenderdocument.entity.TenderRuleNode;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleNodeMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleScoreConfigMapper;
import com.jy.eletender.tenderdocument.service.impl.TenderDocumentRuleServiceImpl;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleTreeAssembler;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleValidator;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderDocumentRuleServiceTest {

    @Mock
    private TenderDocumentMapper tenderDocumentMapper;

    @Mock
    private TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper;

    @Mock
    private TenderRuleHeaderMapper tenderRuleHeaderMapper;

    @Mock
    private TenderRuleNodeMapper tenderRuleNodeMapper;

    @Mock
    private TenderRuleScoreConfigMapper tenderRuleScoreConfigMapper;

    @Mock
    private IProjectLockService projectLockService;

    private TenderDocumentRuleServiceImpl tenderDocumentRuleService;
    private TenderRuleHeader insertedHeader;

    @BeforeEach
    void setUp() {
        insertedHeader = null;
        tenderDocumentRuleService = new TenderDocumentRuleServiceImpl(
                tenderDocumentMapper,
                tenderDocumentSnapshotMapper,
                tenderRuleHeaderMapper,
                tenderRuleNodeMapper,
                tenderRuleScoreConfigMapper,
                projectLockService,
                new TenderDocumentRuleValidator(),
                new TenderDocumentRuleTreeAssembler(),
                new ObjectMapper()
        );
    }

    @Test
    void shouldSaveRuleTreeAsHeaderAndFlatNodes() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        tenderDocument.setStatus("DRAFT");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            TenderRuleHeader header = invocation.getArgument(0);
            header.setId(1L);
            insertedHeader = header;
            return 1;
        }).when(tenderRuleHeaderMapper).insert(any(TenderRuleHeader.class));
        doAnswer(invocation -> {
            TenderRuleNode node = invocation.getArgument(0);
            if (node.getId() == null) {
                node.setId((long) node.getSortNo());
            }
            return 1;
        }).when(tenderRuleNodeMapper).insert(any(TenderRuleNode.class));

        tenderDocumentRuleService.saveRuleTree(100L, "T-01", buildRequest(), buildContext());

        verify(tenderRuleHeaderMapper).insert(any(TenderRuleHeader.class));
        verify(tenderRuleNodeMapper, times(2)).insert(any(TenderRuleNode.class));
        assertThat(insertedHeader.getTotalScore()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(insertedHeader.getWeightRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void shouldResolveReviewModeFromNodeCategoryInsteadOfFrontendInput() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        tenderDocument.setStatus("DRAFT");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            TenderRuleHeader header = invocation.getArgument(0);
            header.setId(1L);
            insertedHeader = header;
            return 1;
        }).when(tenderRuleHeaderMapper).insert(any(TenderRuleHeader.class));
        doAnswer(invocation -> 1).when(tenderRuleNodeMapper).insert(any(TenderRuleNode.class));

        TenderDocumentRuleSaveRequest request = buildRequest();
        request.setNodeCategory("QUALIFICATION");
        request.setReviewMode("SCORE");
        request.setTotalScore(null);
        request.setWeightRate(null);
        request.setTreeToPageData(List.of(buildPassParentRequestNode()));

        tenderDocumentRuleService.saveRuleTree(100L, "T-01", request, buildContext());

        assertThat(insertedHeader).isNotNull();
        assertThat(insertedHeader.getReviewMode()).isEqualTo("PASS");
    }

    @Test
    void shouldNormalizeByDocumentEvalMethodInsteadOfRequestEvalMethod() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        tenderDocument.setStatus("DRAFT");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            TenderRuleHeader header = invocation.getArgument(0);
            header.setId(1L);
            insertedHeader = header;
            return 1;
        }).when(tenderRuleHeaderMapper).insert(any(TenderRuleHeader.class));
        doAnswer(invocation -> 1).when(tenderRuleNodeMapper).insert(any(TenderRuleNode.class));

        TenderDocumentRuleSaveRequest request = buildRequest();
        request.setEvalMethod("LOWEST_PRICE");
        request.setNodeCategory("CREDIT");
        request.setReviewMode("PASS");

        tenderDocumentRuleService.saveRuleTree(100L, "T-01", request, buildContext());

        assertThat(insertedHeader).isNotNull();
        assertThat(insertedHeader.getEvalMethod()).isEqualTo("COMPREHENSIVE_SCORE");
        assertThat(insertedHeader.getReviewMode()).isEqualTo("SCORE");
    }

    @Test
    void shouldRejectIllegalCategoryForEvalMethod() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());

        TenderDocumentRuleSaveRequest request = buildRequest();
        request.setEvalMethod("LOWEST_PRICE");
        request.setNodeCategory("BUSINESS");

        assertThatThrownBy(() -> tenderDocumentRuleService.saveRuleTree(100L, "T-01", request, buildContext()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldAssembleRuleTreeForQuery() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        TenderRuleHeader header = new TenderRuleHeader();
        header.setId(1L);
        header.setTenderDocumentId(100L);
        header.setTenderId("T-01");
        header.setNodeCategory("CREDIT");
        header.setReviewMode("SCORE");
        TenderRuleNode parent = new TenderRuleNode();
        parent.setId(1L);
        parent.setRuleHeaderId(1L);
        parent.setParentId(null);
        parent.setLevel(1);
        parent.setSortNo(1);
        parent.setItemContent("父项");
        parent.setScoreMin(BigDecimal.ZERO);
        parent.setScoreMax(BigDecimal.valueOf(20));
        parent.setLeafFlag(0);
        TenderRuleNode child = new TenderRuleNode();
        child.setId(2L);
        child.setRuleHeaderId(1L);
        child.setParentId(1L);
        child.setLevel(2);
        child.setSortNo(1);
        child.setItemContent("子项");
        child.setScoreMin(BigDecimal.ZERO);
        child.setScoreMax(BigDecimal.valueOf(20));
        child.setLeafFlag(1);
        TenderRuleScoreConfig scoreConfig = new TenderRuleScoreConfig();
        scoreConfig.setTenderDocumentId(100L);
        scoreConfig.setScoreType("WEIGHT");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectOne(any())).thenReturn(header);
        when(tenderRuleNodeMapper.selectList(any())).thenReturn(List.of(parent, child));
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(scoreConfig);

        TenderDocumentRuleResponse response = tenderDocumentRuleService.getRuleTree(100L, "T-01", "CREDIT", buildContext());

        assertThat(response.getTreeToPageData()).hasSize(1);
        assertThat(response.getTreeToPageData().get(0).getChildren()).hasSize(1);
        assertThat(response.getScoreType()).isEqualTo("WEIGHT");
    }

    @Test
    void shouldReturnProjectScoreTypeForPassCategoryRuleTreeQuery() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        TenderRuleHeader header = new TenderRuleHeader();
        header.setId(1L);
        header.setTenderDocumentId(100L);
        header.setTenderId("T-01");
        header.setNodeCategory("QUALIFICATION");
        header.setReviewMode("PASS");
        TenderRuleScoreConfig scoreConfig = new TenderRuleScoreConfig();
        scoreConfig.setTenderDocumentId(100L);
        scoreConfig.setScoreType("ACTUAL");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectOne(any())).thenReturn(header);
        when(tenderRuleNodeMapper.selectList(any())).thenReturn(List.of());
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(scoreConfig);

        TenderDocumentRuleResponse response = tenderDocumentRuleService.getRuleTree(100L, "T-01", "QUALIFICATION", buildContext());

        assertThat(response.getReviewMode()).isEqualTo("PASS");
        assertThat(response.getScoreType()).isEqualTo("ACTUAL");
    }

    @Test
    void shouldReturnEmptyRuleTreeWhenCategoryNotConfiguredYet() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectOne(any())).thenReturn(null);
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(null);

        TenderDocumentRuleResponse response = tenderDocumentRuleService.getRuleTree(100L, "T-01", "CREDIT", buildContext());

        assertThat(response.getTenderId()).isEqualTo("T-01");
        assertThat(response.getEvalMethod()).isEqualTo("COMPREHENSIVE_SCORE");
        assertThat(response.getNodeCategory()).isEqualTo("CREDIT");
        assertThat(response.getReviewMode()).isEqualTo("SCORE");
        assertThat(response.getTreeToPageData()).hasSize(1);
        assertThat(response.getTreeToPageData().get(0).getName()).isNull();
        assertThat(response.getTreeToPageData().get(0).getChildren()).isEmpty();
        assertThat(response.getScoreType()).isNull();
    }

    @Test
    void shouldGetProjectLevelScoreType() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        TenderRuleScoreConfig scoreConfig = new TenderRuleScoreConfig();
        scoreConfig.setTenderDocumentId(100L);
        scoreConfig.setScoreType("ACTUAL");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(scoreConfig);

        TenderDocumentScoreTypeResponse response = tenderDocumentRuleService.getScoreType(100L, buildContext());

        assertThat(response.getScoreType()).isEqualTo("ACTUAL");
    }

    @Test
    void shouldUpdateProjectLevelScoreType() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        tenderDocument.setStatus("DRAFT");
        TenderDocumentScoreTypeSaveRequest request = new TenderDocumentScoreTypeSaveRequest();
        request.setScoreType("WEIGHT");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleScoreConfigMapper.selectOne(any())).thenReturn(null);

        tenderDocumentRuleService.updateScoreType(100L, request, buildContext());

        verify(tenderRuleScoreConfigMapper).insert(any(TenderRuleScoreConfig.class));
    }

    @Test
    void shouldSortTenderListAndAppendIndexOfDescForRulePage() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode("BASIC_INFO");
        snapshot.setSnapshotJson("""
                {
                  "tenderList": [
                    {"tenderId":"T-02","tenderName":"二标段","indexOf":2},
                    {"tenderId":"T-01","tenderName":"一标段","indexOf":1}
                  ]
                }
                """);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentSnapshotMapper.selectOne(any())).thenReturn(snapshot);

        TenderDocumentEvaluationRulesPageResponse response = tenderDocumentRuleService.getRulePage(100L, buildContext());

        assertThat(response.getTenderList()).hasSize(2);
        assertThat(response.getTenderList().get(0)).containsEntry("tenderId", "T-01");
        assertThat(response.getTenderList().get(0)).containsEntry("indexOfDesc", "标段一");
        assertThat(response.getTenderList().get(1)).containsEntry("tenderId", "T-02");
        assertThat(response.getTenderList().get(1)).containsEntry("indexOfDesc", "标段二");
    }

    private TenderDocumentRuleSaveRequest buildRequest() {
        TenderDocumentRuleTreeNode child = new TenderDocumentRuleTreeNode();
        child.setId("child-1");
        child.setOrder(1);
        child.setKey("1.1");
        child.setName("子项");
        child.setLowest(BigDecimal.ZERO);
        child.setHighest(BigDecimal.TEN);
        child.setStandard("评分标准");
        child.setParent(false);
        child.setObjectiveType("SUBJECTIVE");
        child.setChildren(List.of());

        TenderDocumentRuleTreeNode parent = new TenderDocumentRuleTreeNode();
        parent.setId("parent-1");
        parent.setOrder(1);
        parent.setKey("1");
        parent.setName("父项");
        parent.setLowest(BigDecimal.ZERO);
        parent.setHighest(BigDecimal.TEN);
        parent.setParent(true);
        parent.setChildren(List.of(child));

        TenderDocumentRuleSaveRequest request = new TenderDocumentRuleSaveRequest();
        request.setTenderId("T-01");
        request.setTenderName("一标段");
        request.setEvalMethod("COMPREHENSIVE_SCORE");
        request.setNodeCategory("CREDIT");
        request.setReviewMode("SCORE");
        request.setTotalScore(BigDecimal.TEN);
        request.setWeightRate(BigDecimal.valueOf(100));
        request.setTreeToPageData(List.of(parent));
        return request;
    }

    private TenderDocumentRuleTreeNode buildPassParentRequestNode() {
        TenderDocumentRuleTreeNode child = new TenderDocumentRuleTreeNode();
        child.setId("child-pass");
        child.setOrder(1);
        child.setKey("1.1");
        child.setName("资格项");
        child.setLowest(BigDecimal.ZERO);
        child.setHighest(BigDecimal.ZERO);
        child.setStandard("符合即可");
        child.setParent(false);
        child.setChildren(List.of());

        TenderDocumentRuleTreeNode parent = new TenderDocumentRuleTreeNode();
        parent.setId("parent-pass");
        parent.setOrder(1);
        parent.setKey("1");
        parent.setName("资格审查");
        parent.setLowest(BigDecimal.ZERO);
        parent.setHighest(BigDecimal.ZERO);
        parent.setParent(true);
        parent.setChildren(List.of(child));
        return parent;
    }

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("app-a");
        context.setUserId("u-1");
        context.setUserName("测试用户");
        context.setEnterpriseCode("913301");
        return context;
    }
}
