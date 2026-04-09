package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeResponse;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;
import com.jy.eletender.common.interaction.dto.ProjectTenderInfo;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.support.TenderDocumentBasicInfoSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentBidRecordSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionTenderDocumentSyncGatewayTest {

    @Mock
    private BusinessSystemRemoteClient remoteClient;

    @InjectMocks
    private InteractionTenderDocumentSyncGateway gateway;

    @Test
    void shouldMapProjectBasicInfoToSyncResult() {
        ProjectBasicInfoResponse response = new ProjectBasicInfoResponse();
        response.setProjectId("P-100");
        response.setProjectNo("PN-100");
        response.setProjectName("示例项目");
        response.setProjectType(InteractionProjectType.PUBLIC);
        response.setPurchaserName("测试采购单位");
        response.setPurchaseMethod("公开招标");
        response.setBidEndTime("2026-06-01 10:00:00");
        response.setEvalMethod(InteractionEvalMethod.LOWEST_PRICE);
        response.setTenderList(List.of(buildTender("T-01", 1, "一标段", "TN-01", "一标段", "100.00", "一标段采购内容"),
                buildTender("T-02", 2, "二标段", "TN-02", "二标段", "200.00", "二标段采购内容")));
        when(remoteClient.queryProjectBasicInfo(any(), any(), any(), any(ProjectBasicInfoQueryRequest.class))).thenReturn(response);

        TenderDocumentEntryRequest request = new TenderDocumentEntryRequest();
        request.setBizType(1);
        request.setBizId("BIZ-1");
        request.setProjectId("P-100");
        request.setTenderId("T-01");

        TenderDocumentBasicInfoSyncResult result = gateway.syncBasicInfo(request, buildContext());

        assertThat(result.getProjectCode()).isEqualTo("PN-100");
        assertThat(result.getPurchaseMethod()).isEqualTo("公开招标");
        assertThat(result.getEvalMethod()).isEqualTo(InteractionEvalMethod.LOWEST_PRICE);
        assertThat(result.getCompileScope()).isEqualTo(TenderDocumentScopeType.PROJECT.name());
        assertThat(result.getTenderList()).hasSize(2);
        assertThat(result.getTenderList().get(0)).containsEntry("tenderId", "T-01");
        assertThat(result.getTenderList().get(0)).containsEntry("indexOf", 1);
        assertThat(result.getTenderList().get(0)).containsEntry("indexOfDesc", "一标段");
        assertThat(result.getTenderList().get(0)).containsEntry("tenderNo", "TN-01");
        assertThat(result.getTenderList().get(0)).containsEntry("purchaseContext", "一标段采购内容");
        assertThat(result.getTenderList().get(1)).containsEntry("tenderAmount", new BigDecimal("200.00"));
        assertThat(result.getProjectInfo()).containsEntry("projectNo", "PN-100");
        assertThat(result.getProjectInfo()).containsEntry("purchaseMethod", "公开招标");
        assertThat(result.getProjectInfo()).containsEntry("purchaserName", "测试采购单位");
        assertThat(result.getProjectInfo()).containsEntry("bidEndTime", "2026-06-01 10:00:00");
    }

    @Test
    void shouldResolveCompileScopeFromProjectTypeWhenRemoteReturnsConflictingScope() {
        ProjectBasicInfoResponse response = new ProjectBasicInfoResponse();
        response.setProjectId("P-100");
        response.setProjectNo("PN-100");
        response.setProjectName("示例项目");
        response.setProjectType(InteractionProjectType.INVITE);
        response.setPurchaseMethod("邀请招标");
        response.setEvalMethod(InteractionEvalMethod.LOWEST_PRICE);
        when(remoteClient.queryProjectBasicInfo(any(), any(), any(), any(ProjectBasicInfoQueryRequest.class))).thenReturn(response);

        TenderDocumentEntryRequest request = new TenderDocumentEntryRequest();
        request.setBizType(1);
        request.setBizId("BIZ-1");
        request.setProjectId("P-100");
        request.setTenderId("T-01");

        TenderDocumentBasicInfoSyncResult result = gateway.syncBasicInfo(request, buildContext());

        assertThat(result.getCompileScope()).isEqualTo(TenderDocumentScopeType.TENDER.name());
        assertThat(result.getProjectInfo()).containsEntry("compileScope", TenderDocumentScopeType.TENDER.name());
    }

    @Test
    void shouldMapBidRecordSchemeToSnapshotPayload() {
        ProjectBasicInfoResponse basicInfoResponse = new ProjectBasicInfoResponse();
        basicInfoResponse.setTenderList(List.of(buildTender("T-01", 1, "一标段", "TN-01", "一标段", "100.00", "一标段采购内容")));
        when(remoteClient.queryProjectBasicInfo(any(), any(), any(), any(ProjectBasicInfoQueryRequest.class))).thenReturn(basicInfoResponse);

        BidRecordSchemeResponse response = new BidRecordSchemeResponse();
        response.setSchemeContent("[{\"sign\":\"remark\",\"description\":\"备注\",\"indexOf\":0,\"mustFlag\":false,\"selected\":true,\"controlLength\":5,\"unit\":\"\"}]");
        when(remoteClient.queryBidRecordScheme(any(), any(), any(), any(BidRecordSchemeQueryRequest.class))).thenReturn(response);

        TenderDocument document = new TenderDocument();
        document.setBizType(1);
        document.setBizId("BIZ-1");
        document.setProjectId("P-100");
        document.setTenderId("T-01");
        document.setProjectName("示例项目");
        document.setPurchaseMethod("公开招标");
        document.setCompileScope("PROJECT");

        TenderDocumentBidRecordSyncResult result = gateway.syncBidRecord(document, buildContext());

        assertThat(result.getBidRecord()).isEqualTo(response);
        assertThat(result.getProjectInfo()).containsEntry("projectId", "P-100");
        assertThat(result.getTenderList()).hasSize(1);
        assertThat(result.getTenderList().get(0)).containsEntry("tenderId", "T-01");
    }

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("demo-app");
        context.setUserId("u-1");
        context.setUserName("测试用户");
        return context;
    }

    private ProjectTenderInfo buildTender(String tenderId, Integer indexOf, String indexOfDesc, String tenderNo, String tenderName, String tenderAmount, String purchaseContext) {
        ProjectTenderInfo tender = new ProjectTenderInfo();
        tender.setTenderId(tenderId);
        tender.setIndexOf(indexOf);
        tender.setIndexOfDesc(indexOfDesc);
        tender.setTenderNo(tenderNo);
        tender.setTenderName(tenderName);
        tender.setTenderAmount(new BigDecimal(tenderAmount));
        tender.setPurchaseContext(purchaseContext);
        return tender;
    }
}
