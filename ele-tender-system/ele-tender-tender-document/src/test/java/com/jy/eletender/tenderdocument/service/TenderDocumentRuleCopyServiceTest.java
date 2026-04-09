package com.jy.eletender.tenderdocument.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleCopyRequest;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderDocumentRuleCopyServiceTest {

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

    @BeforeEach
    void setUp() {
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
    void shouldCopySourceTenderRulesToTargetTender() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setStatus("DRAFT");

        TenderRuleHeader sourceHeader = new TenderRuleHeader();
        sourceHeader.setId(10L);
        sourceHeader.setTenderDocumentId(100L);
        sourceHeader.setProjectId("P-100");
        sourceHeader.setTenderId("T-01");
        sourceHeader.setTenderName("一标段");
        sourceHeader.setNodeCategory("CREDIT");
        sourceHeader.setEvalMethod("COMPREHENSIVE_SCORE");
        sourceHeader.setReviewMode("SCORE");

        TenderRuleNode sourceParent = new TenderRuleNode();
        sourceParent.setId(1001L);
        sourceParent.setRuleHeaderId(10L);
        sourceParent.setTenderId("T-01");
        sourceParent.setLevel(1);
        sourceParent.setSortNo(1);
        sourceParent.setItemContent("父节点");
        sourceParent.setLeafFlag(0);

        TenderRuleNode sourceChild = new TenderRuleNode();
        sourceChild.setId(1002L);
        sourceChild.setRuleHeaderId(10L);
        sourceChild.setTenderId("T-01");
        sourceChild.setParentId(1001L);
        sourceChild.setLevel(2);
        sourceChild.setSortNo(1);
        sourceChild.setItemContent("子节点");
        sourceChild.setLeafFlag(1);

        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of(sourceHeader), List.of());
        when(tenderRuleNodeMapper.selectList(any())).thenReturn(List.of(sourceParent, sourceChild));
        doAnswer(invocation -> {
            TenderRuleHeader header = invocation.getArgument(0);
            header.setId(20L);
            return 1;
        }).when(tenderRuleHeaderMapper).insert(any(TenderRuleHeader.class));
        doAnswer(invocation -> {
            TenderRuleNode node = invocation.getArgument(0);
            if (node.getParentId() == null) {
                node.setId(2001L);
            } else {
                node.setId(2002L);
            }
            return 1;
        }).when(tenderRuleNodeMapper).insert(any(TenderRuleNode.class));

        TenderDocumentRuleCopyRequest request = new TenderDocumentRuleCopyRequest();
        request.setSourceTenderId("T-01");
        request.setTargetTenderId("T-02");
        request.setTargetTenderName("二标段");

        tenderDocumentRuleService.copyRuleTree(100L, request, buildContext());

        verify(tenderRuleHeaderMapper).insert(any(TenderRuleHeader.class));
        verify(tenderRuleNodeMapper, times(2)).insert(any(TenderRuleNode.class));
        verify(tenderRuleScoreConfigMapper, never()).insert(any());
        verify(tenderRuleScoreConfigMapper, never()).updateById(any());
    }

    @Test
    void shouldUpdateExistingTargetHeaderWhenSameCategoryAlreadyExists() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setStatus("DRAFT");

        TenderRuleHeader sourceHeader = new TenderRuleHeader();
        sourceHeader.setId(10L);
        sourceHeader.setTenderDocumentId(100L);
        sourceHeader.setProjectId("P-100");
        sourceHeader.setTenderId("T-01");
        sourceHeader.setTenderName("一标段");
        sourceHeader.setNodeCategory("TECHNICAL");
        sourceHeader.setEvalMethod("COMPREHENSIVE_SCORE");
        sourceHeader.setReviewMode("SCORE");

        TenderRuleHeader targetHeader = new TenderRuleHeader();
        targetHeader.setId(20L);
        targetHeader.setTenderDocumentId(100L);
        targetHeader.setProjectId("P-100");
        targetHeader.setTenderId("T-02");
        targetHeader.setTenderName("旧二标段");
        targetHeader.setNodeCategory("TECHNICAL");
        targetHeader.setEvalMethod("COMPREHENSIVE_SCORE");
        targetHeader.setReviewMode("SCORE");

        TenderRuleNode sourceNode = new TenderRuleNode();
        sourceNode.setId(1001L);
        sourceNode.setRuleHeaderId(10L);
        sourceNode.setTenderId("T-01");
        sourceNode.setLevel(1);
        sourceNode.setSortNo(1);
        sourceNode.setItemContent("技术节点");
        sourceNode.setLeafFlag(1);

        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of(sourceHeader), List.of(targetHeader));
        when(tenderRuleNodeMapper.selectList(any())).thenReturn(List.of(sourceNode));
        doAnswer(invocation -> {
            TenderRuleNode node = invocation.getArgument(0);
            node.setId(2001L);
            return 1;
        }).when(tenderRuleNodeMapper).insert(any(TenderRuleNode.class));

        TenderDocumentRuleCopyRequest request = new TenderDocumentRuleCopyRequest();
        request.setSourceTenderId("T-01");
        request.setTargetTenderId("T-02");
        request.setTargetTenderName("二标段");

        tenderDocumentRuleService.copyRuleTree(100L, request, buildContext());

        verify(tenderRuleHeaderMapper, never()).delete(any());
        verify(tenderRuleHeaderMapper, never()).insert(any(TenderRuleHeader.class));
        verify(tenderRuleHeaderMapper).updateById(any(TenderRuleHeader.class));
        verify(tenderRuleNodeMapper).delete(any());
        verify(tenderRuleNodeMapper).insert(any(TenderRuleNode.class));
        verify(tenderRuleScoreConfigMapper, never()).insert(any());
        verify(tenderRuleScoreConfigMapper, never()).updateById(any());
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
