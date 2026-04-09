package com.jy.eletender.tenderdocument.support.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.tenderdocument.model.generation.FinalPackagePayload;
import com.jy.eletender.tenderdocument.model.generation.FinalPackageRulePayload;
import com.jy.eletender.tenderdocument.model.generation.FinalPackageTenderPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinalPackagePayloadSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeFinalPackagePayloadWithExpectedTopLevelKeys() throws Exception {
        FinalPackagePayload payload = buildPayload();

        String json = objectMapper.writeValueAsString(payload);

        assertThat(json).contains("\"baseInfo\"");
        assertThat(json).contains("\"tenders\"");
        assertThat(json).contains("\"tenderDocumentSignPdf\"");
        assertThat(json).contains("\"caKeysInfo\"");
        assertThat(json).contains("\"settings\"");
        assertThat(json).contains("\"versionInfo\"");
        assertThat(json).contains("\"purchaserName\"");
        assertThat(json).contains("\"purchaseMethod\"");
        assertThat(json).contains("\"bidEndTime\"");
        assertThat(json).contains("\"tenderAmount\"");
        assertThat(json).contains("\"purchaseContext\"");
    }

    @Test
    void shouldSerializeBidEvalRulesWithEnumSectionsAndObjectiveType() throws Exception {
        FinalPackagePayload payload = buildPayload();

        String json = objectMapper.writeValueAsString(payload);

        assertThat(json).contains("\"QUALIFICATION\"");
        assertThat(json).contains("\"CONFORMITY\"");
        assertThat(json).contains("\"DETAIL\"");
        assertThat(json).contains("\"CREDIT\"");
        assertThat(json).contains("\"TECHNICAL\"");
        assertThat(json).contains("\"BUSINESS\"");
        assertThat(json).contains("\"objectiveType\"");
        assertThat(json).doesNotContain("\"ojectiveType\"");
    }

    private FinalPackagePayload buildPayload() {
        FinalPackagePayload payload = new FinalPackagePayload();

        FinalPackagePayload.BaseInfo baseInfo = new FinalPackagePayload.BaseInfo();
        baseInfo.setProjectId("P-100");
        baseInfo.setProjectName("项目一");
        baseInfo.setProjectNo("XM-001");
        baseInfo.setPurchaserName("测试采购单位");
        baseInfo.setPurchaseMethod("公开招标");
        baseInfo.setBidEndTime("2026-06-01 10:00:00");
        FinalPackagePayload.TenderInfo tenderInfo = new FinalPackagePayload.TenderInfo();
        tenderInfo.setTenderId("T-01");
        tenderInfo.setTenderName("一标段");
        tenderInfo.setTenderNo("BD-01");
        tenderInfo.setTenderAmount(new java.math.BigDecimal("100.00"));
        tenderInfo.setPurchaseContext("一标段采购内容");
        baseInfo.setTendersInfo(List.of(tenderInfo));
        payload.setBaseInfo(baseInfo);

        FinalPackageTenderPayload tenderPayload = new FinalPackageTenderPayload();
        tenderPayload.setTenderId("T-01");
        tenderPayload.setTenderName("一标段");
        tenderPayload.setTenderNo("BD-01");
        tenderPayload.setBidForms(Map.of("schemeContent", "[{\"sign\":\"bidPrice\"}]"));
        tenderPayload.setBidEvalRules(buildRules());
        payload.setTenders(List.of(tenderPayload));

        FinalPackagePayload.TenderDocumentSignPdf signPdf = new FinalPackagePayload.TenderDocumentSignPdf();
        signPdf.setFileName("招标文件.pdf");
        signPdf.setSize(1024L);
        signPdf.setSignedFileBase64("cGRm");
        signPdf.setSha256("sha256-demo");
        payload.setTenderDocumentSignPdf(signPdf);

        FinalPackagePayload.CaKeyInfo caKeyInfo = new FinalPackagePayload.CaKeyInfo();
        caKeyInfo.setEncryptOrder(1);
        caKeyInfo.setUserId("u-1");
        caKeyInfo.setCaId("ca-id");
        caKeyInfo.setCaNo("ca-no");
        caKeyInfo.setPublicKey("public-key");
        payload.setCaKeysInfo(List.of(caKeyInfo));

        FinalPackagePayload.Settings settings = new FinalPackagePayload.Settings();
        FinalPackagePayload.SignPosition signPosition = new FinalPackagePayload.SignPosition();
        signPosition.setX(200);
        signPosition.setY(200);
        settings.setSignPosition(signPosition);
        payload.setSettings(settings);

        FinalPackagePayload.VersionInfo versionInfo = new FinalPackagePayload.VersionInfo();
        versionInfo.setTenderDocumentFormatVersion("0.0.1");
        versionInfo.setTenderDocumentAppVersion("0.0.1");
        versionInfo.setTenderDocumentUniqueCode("unique-code");
        payload.setVersionInfo(versionInfo);
        return payload;
    }

    private FinalPackageRulePayload buildRules() {
        FinalPackageRulePayload rules = new FinalPackageRulePayload();
        FinalPackageRulePayload.Info info = new FinalPackageRulePayload.Info();
        info.setBidEvalMethod("综合评分法");
        info.setReviewMode("ACTUAL");
        rules.setInfo(info);

        FinalPackageRulePayload.RuleSection qualification = new FinalPackageRulePayload.RuleSection();
        qualification.setReviewMode("PASS");
        qualification.setScoreRules(List.of(buildRuleNode("q-1")));
        rules.setQualification(qualification);

        FinalPackageRulePayload.RuleSection conformity = new FinalPackageRulePayload.RuleSection();
        conformity.setReviewMode("PASS");
        conformity.setScoreRules(List.of(buildRuleNode("c-1")));
        rules.setConformity(conformity);

        FinalPackageRulePayload.RuleSection detail = new FinalPackageRulePayload.RuleSection();
        detail.setReviewMode("PASS");
        detail.setScoreRules(List.of(buildRuleNode("d-1")));
        rules.setDetail(detail);

        FinalPackageRulePayload.ScoreRuleSection credit = new FinalPackageRulePayload.ScoreRuleSection();
        credit.setReviewMode("SCORE");
        credit.setTotalScore(100);
        credit.setPercentage(30);
        credit.setScoreRules(List.of(buildRuleNode("cr-1")));
        rules.setCredit(credit);

        FinalPackageRulePayload.ScoreRuleSection technical = new FinalPackageRulePayload.ScoreRuleSection();
        technical.setReviewMode("SCORE");
        technical.setTotalScore(100);
        technical.setPercentage(30);
        technical.setScoreRules(List.of(buildRuleNode("t-1")));
        rules.setTechnical(technical);

        FinalPackageRulePayload.ScoreRuleSection business = new FinalPackageRulePayload.ScoreRuleSection();
        business.setReviewMode("SCORE");
        business.setTotalScore(100);
        business.setPercentage(40);
        business.setScoreRules(List.of(buildRuleNode("b-1")));
        rules.setBusiness(business);
        return rules;
    }

    private FinalPackageRulePayload.RuleNode buildRuleNode(String id) {
        FinalPackageRulePayload.RuleNode ruleNode = new FinalPackageRulePayload.RuleNode();
        ruleNode.setId(id);
        ruleNode.setOrder(1);
        ruleNode.setKey("1");
        ruleNode.setName("规则" + id);
        ruleNode.setLowest(BigDecimal.ZERO);
        ruleNode.setHighest(BigDecimal.TEN);
        ruleNode.setObjectiveType("OBJECTIVE");
        ruleNode.setChildren(List.of());
        ruleNode.setIsParent(Boolean.FALSE);
        return ruleNode;
    }
}
