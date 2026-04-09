package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeResponse;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;
import com.jy.eletender.common.interaction.dto.ProjectTenderInfo;
import com.jy.eletender.common.logging.TraceContext;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.support.TenderDocumentBasicInfoSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentBidRecordSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentCompileScopeResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentSyncGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对接业务系统的同步网关实现。
 * 通过交互协议拉取项目基础信息与开标标录方案，并映射为编制模块内部结构。
 */
@Primary
@Component
public class InteractionTenderDocumentSyncGateway implements TenderDocumentSyncGateway {

    private final BusinessSystemRemoteClient remoteClient;

    public InteractionTenderDocumentSyncGateway(BusinessSystemRemoteClient remoteClient) {
        this.remoteClient = remoteClient;
    }

    /**
     * 编制入口场景同步项目基本信息。
     */
    @Override
    public TenderDocumentBasicInfoSyncResult syncBasicInfo(TenderDocumentEntryRequest request, TenderDocumentUserContext userContext) {
        ProjectBasicInfoQueryRequest query = new ProjectBasicInfoQueryRequest();
        query.setBizType(request.getBizType());
        query.setBizId(request.getBizId());
        query.setProjectId(request.getProjectId());
        query.setTenderId(request.getTenderId());
        return mapBasicInfo(remoteClient.queryProjectBasicInfo(userContext.getAppKey(), TraceContext.getTraceId(), userContext.getAuthorization(), query));
    }

    /**
     * 已有编制单场景同步项目基本信息。
     */
    @Override
    public TenderDocumentBasicInfoSyncResult syncBasicInfo(TenderDocument tenderDocument, TenderDocumentUserContext userContext) {
        ProjectBasicInfoQueryRequest query = new ProjectBasicInfoQueryRequest();
        query.setBizType(tenderDocument.getBizType());
        query.setBizId(tenderDocument.getBizId());
        query.setProjectId(tenderDocument.getProjectId());
        query.setTenderId(tenderDocument.getTenderId());
        return mapBasicInfo(remoteClient.queryProjectBasicInfo(userContext.getAppKey(), TraceContext.getTraceId(), userContext.getAuthorization(), query));
    }

    /**
     * 同步标录方案。
     * 先拉取基础项目信息用于页面头，再拉取标录方案主体数据。
     */
    @Override
    public TenderDocumentBidRecordSyncResult syncBidRecord(TenderDocument tenderDocument, TenderDocumentUserContext userContext) {
        ProjectBasicInfoQueryRequest basicInfoQuery = new ProjectBasicInfoQueryRequest();
        basicInfoQuery.setBizType(tenderDocument.getBizType());
        basicInfoQuery.setBizId(tenderDocument.getBizId());
        basicInfoQuery.setProjectId(tenderDocument.getProjectId());
        basicInfoQuery.setTenderId(tenderDocument.getTenderId());
        ProjectBasicInfoResponse basicInfo = remoteClient.queryProjectBasicInfo(userContext.getAppKey(), TraceContext.getTraceId(), userContext.getAuthorization(), basicInfoQuery);

        BidRecordSchemeQueryRequest query = new BidRecordSchemeQueryRequest();
        query.setBizType(tenderDocument.getBizType());
        query.setBizId(tenderDocument.getBizId());
        query.setProjectId(tenderDocument.getProjectId());
        query.setTenderId(tenderDocument.getTenderId());
        BidRecordSchemeResponse scheme = remoteClient.queryBidRecordScheme(userContext.getAppKey(), TraceContext.getTraceId(), userContext.getAuthorization(), query);

        TenderDocumentBidRecordSyncResult result = new TenderDocumentBidRecordSyncResult();
        Map<String, Object> projectInfo = new LinkedHashMap<String, Object>();
        projectInfo.put("projectId", tenderDocument.getProjectId());
        projectInfo.put("projectName", tenderDocument.getProjectName());
        projectInfo.put("purchaseMethod", tenderDocument.getPurchaseMethod());
        projectInfo.put("compileScope", tenderDocument.getCompileScope());
        result.setProjectInfo(projectInfo);
        // 标段列表以基础信息查询结果为准，确保与项目主数据保持一致。
        appendTenderList(result.getTenderList(), basicInfo.getTenderList());
        result.setBidRecord(scheme);
        return result;
    }

    private TenderDocumentBasicInfoSyncResult mapBasicInfo(ProjectBasicInfoResponse response) {
        TenderDocumentBasicInfoSyncResult result = new TenderDocumentBasicInfoSyncResult();
        // compileScope 不直接信任外部字符串，统一按项目类型推导。
        String resolvedCompileScope = TenderDocumentCompileScopeResolver.resolve(response.getProjectType());
        result.setCompileScope(resolvedCompileScope);
        result.setProjectType(response.getProjectType());
        result.setProjectCode(response.getProjectNo());
        result.setProjectName(response.getProjectName());
        result.setPurchaseMethod(response.getPurchaseMethod());
        result.setEvalMethod(response.getEvalMethod());

        Map<String, Object> projectInfo = new LinkedHashMap<String, Object>();
        projectInfo.put("projectId", response.getProjectId());
        projectInfo.put("projectNo", response.getProjectNo());
        projectInfo.put("projectName", response.getProjectName());
        projectInfo.put("projectType", response.getProjectType() == null ? null : response.getProjectType().name());
        projectInfo.put("compileScope", resolvedCompileScope);
        projectInfo.put("purchaseMethod", result.getPurchaseMethod());
        projectInfo.put("purchaserName", response.getPurchaserName());
        projectInfo.put("bidEndTime", response.getBidEndTime());
        result.setProjectInfo(projectInfo);
        appendTenderList(result.getTenderList(), response.getTenderList());
        return result;
    }

    private void appendTenderList(List<Map<String, Object>> target, List<ProjectTenderInfo> source) {
        if (source == null) {
            return;
        }
        for (ProjectTenderInfo tender : source) {
            Map<String, Object> tenderInfo = new LinkedHashMap<String, Object>();
            tenderInfo.put("tenderId", tender.getTenderId());
            tenderInfo.put("indexOf", tender.getIndexOf());
            tenderInfo.put("indexOfDesc", tender.getIndexOfDesc());
            tenderInfo.put("tenderNo", tender.getTenderNo());
            tenderInfo.put("tenderName", tender.getTenderName());
            tenderInfo.put("tenderAmount", tender.getTenderAmount());
            tenderInfo.put("purchaseContext", tender.getPurchaseContext());
            target.add(tenderInfo);
        }
    }
}
