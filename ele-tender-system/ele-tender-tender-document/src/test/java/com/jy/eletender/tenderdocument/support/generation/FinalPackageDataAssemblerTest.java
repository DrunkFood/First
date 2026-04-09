package com.jy.eletender.tenderdocument.support.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentSnapshot;
import com.jy.eletender.tenderdocument.entity.TenderRuleHeader;
import com.jy.eletender.tenderdocument.entity.TenderRuleNode;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentSnapshotMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleNodeMapper;
import com.jy.eletender.tenderdocument.model.generation.FinalPackagePayload;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleTreeAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalPackageDataAssemblerTest {

    @Mock
    private TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper;

    @Mock
    private TenderRuleHeaderMapper tenderRuleHeaderMapper;

    @Mock
    private TenderRuleNodeMapper tenderRuleNodeMapper;

    private FinalPackageDataAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new FinalPackageDataAssembler(
                tenderDocumentSnapshotMapper,
                tenderRuleHeaderMapper,
                tenderRuleNodeMapper,
                new TenderDocumentRuleTreeAssembler(),
                new ObjectMapper()
        );
    }

    @Test
    void shouldAssembleProjectScopedPayloadWithAllTenders() throws Exception {
        TenderDocument tenderDocument = buildDocument(TenderDocumentScopeType.PROJECT, null);
        when(tenderDocumentSnapshotMapper.selectOne(any()))
                .thenReturn(snapshot("BASIC_INFO", basicInfoSnapshotJson()), snapshot("BID_RECORD", bidRecordSnapshotJson()));
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        FinalPackagePayload payload = assembler.assemble(tenderDocument);

        assertThat(payload.getBaseInfo().getTendersInfo()).hasSize(2);
        assertThat(payload.getTenders()).hasSize(2);
        assertThat(payload.getTenders()).extracting(tender -> tender.getTenderId()).containsExactly("T-01", "T-02");
        assertThat(payload.getTenders().get(0).getBidForms()).containsEntry("schemeContent", "[{\"sign\":\"bidPrice\"}]");
        assertThat(payload.getTenders().get(1).getBidForms()).containsEntry("schemeContent", "[{\"sign\":\"ratePrice\"}]");
        assertThat(payload.getBaseInfo().getPurchaserName()).isEqualTo("测试采购单位");
        assertThat(payload.getBaseInfo().getPurchaseMethod()).isEqualTo("公开招标");
        assertThat(payload.getBaseInfo().getBidEndTime()).isEqualTo("2026-06-01 10:00:00");
        assertThat(payload.getBaseInfo().getTendersInfo().get(0).getPurchaseContext()).isEqualTo("一标段采购内容");
        assertThat(payload.getBaseInfo().getTendersInfo().get(1).getPurchaseContext()).isEqualTo("二标段采购内容");
        assertThat(payload.getBaseInfo().getTendersInfo().get(0).getTenderAmount()).isNotNull();
    }

    @Test
    void shouldAssembleTenderScopedPayloadWithCurrentTenderOnly() throws Exception {
        TenderDocument tenderDocument = buildDocument(TenderDocumentScopeType.TENDER, "T-02");
        when(tenderDocumentSnapshotMapper.selectOne(any()))
                .thenReturn(snapshot("BASIC_INFO", basicInfoSnapshotJson()), snapshot("BID_RECORD", bidRecordSnapshotJson()));
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of());

        FinalPackagePayload payload = assembler.assemble(tenderDocument);

        assertThat(payload.getBaseInfo().getTendersInfo()).hasSize(1);
        assertThat(payload.getBaseInfo().getTendersInfo().get(0).getTenderId()).isEqualTo("T-02");
        assertThat(payload.getTenders()).hasSize(1);
        assertThat(payload.getTenders().get(0).getTenderId()).isEqualTo("T-02");
        assertThat(payload.getTenders().get(0).getBidForms()).containsEntry("schemeContent", "[{\"sign\":\"ratePrice\"}]");
    }

    @Test
    void shouldMapDetailCategoryUsingPassStyleRuleSection() throws Exception {
        TenderDocument tenderDocument = buildDocument(TenderDocumentScopeType.TENDER, "T-01");
        when(tenderDocumentSnapshotMapper.selectOne(any()))
                .thenReturn(snapshot("BASIC_INFO", basicInfoSnapshotJson()), snapshot("BID_RECORD", bidRecordSnapshotJson()));
        when(tenderRuleHeaderMapper.selectList(any())).thenReturn(List.of(detailHeader()));
        when(tenderRuleNodeMapper.selectList(any())).thenReturn(List.of(detailNode()));

        FinalPackagePayload payload = assembler.assemble(tenderDocument);

        assertThat(payload.getTenders()).hasSize(1);
        assertThat(payload.getTenders().get(0).getBidEvalRules().getDetail()).isNotNull();
        assertThat(payload.getTenders().get(0).getBidEvalRules().getDetail().getReviewMode()).isEqualTo("PASS");
        assertThat(payload.getTenders().get(0).getBidEvalRules().getDetail().getScoreRules()).hasSize(1);
        assertThat(payload.getTenders().get(0).getBidEvalRules().getDetail().getScoreRules().get(0).getObjectiveType())
                .isEqualTo("OBJECTIVE");
        assertThat(payload.getTenders().get(0).getBidEvalRules().getCredit()).isNull();
    }

    private TenderDocument buildDocument(TenderDocumentScopeType scopeType, String tenderId) {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setProjectName("项目一");
        tenderDocument.setProjectCode("XM-001");
        tenderDocument.setCompileScope(scopeType.name());
        tenderDocument.setTenderId(tenderId);
        tenderDocument.setEvalMethod("COMPREHENSIVE_SCORE");
        return tenderDocument;
    }

    private TenderDocumentSnapshot snapshot(String stepCode, String snapshotJson) {
        TenderDocumentSnapshot snapshot = new TenderDocumentSnapshot();
        snapshot.setTenderDocumentId(100L);
        snapshot.setStepCode(stepCode);
        snapshot.setSnapshotJson(snapshotJson);
        return snapshot;
    }

    private String basicInfoSnapshotJson() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "projectInfo", Map.of(
                        "projectId", "P-100",
                        "purchaserName", "测试采购单位",
                        "purchaseMethod", "公开招标",
                        "bidEndTime", "2026-06-01 10:00:00"
                ),
                "tenderList", List.of(
                        Map.of("tenderId", "T-01", "tenderName", "一标段", "tenderNo", "BD-01",
                                "tenderAmount", new java.math.BigDecimal("100.00"), "purchaseContext", "一标段采购内容"),
                        Map.of("tenderId", "T-02", "tenderName", "二标段", "tenderNo", "BD-02",
                                "tenderAmount", new java.math.BigDecimal("200.00"), "purchaseContext", "二标段采购内容")
                )
        ));
    }

    private String bidRecordSnapshotJson() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "bidRecord", Map.of("schemeContent", "[{\"sign\":\"remark\"}]"),
                "tenderList", List.of(
                        Map.of("tenderId", "T-01", "bidRecord", Map.of("schemeContent", "[{\"sign\":\"bidPrice\"}]")),
                        Map.of("tenderId", "T-02", "bidRecord", Map.of("schemeContent", "[{\"sign\":\"ratePrice\"}]"))
                )
        ));
    }

    private TenderRuleHeader detailHeader() {
        TenderRuleHeader header = new TenderRuleHeader();
        header.setId(10L);
        header.setTenderDocumentId(100L);
        header.setTenderId("T-01");
        header.setTenderName("一标段");
        header.setEvalMethod("LOWEST_PRICE");
        header.setNodeCategory("DETAIL");
        header.setReviewMode("PASS");
        header.setTotalScore(BigDecimal.ZERO);
        header.setWeightRate(BigDecimal.ZERO);
        return header;
    }

    private TenderRuleNode detailNode() {
        TenderRuleNode node = new TenderRuleNode();
        node.setId(101L);
        node.setRuleHeaderId(10L);
        node.setTenderId("T-01");
        node.setLevel(1);
        node.setSortNo(1);
        node.setItemContent("详细评审项");
        node.setScoreMin(BigDecimal.ZERO);
        node.setScoreMax(BigDecimal.TEN);
        node.setScoreAttribute("OBJECTIVE");
        node.setLeafFlag(1);
        return node;
    }
}
