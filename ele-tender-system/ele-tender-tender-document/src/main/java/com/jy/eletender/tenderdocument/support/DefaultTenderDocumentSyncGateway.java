package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一期占位实现，后续替换为正式业务系统交互实现。
 */
@Component
@ConditionalOnMissingBean(TenderDocumentSyncGateway.class)
public class DefaultTenderDocumentSyncGateway implements TenderDocumentSyncGateway {

    private static final BigDecimal DEFAULT_TENDER_AMOUNT = new BigDecimal("1000000.00");

    /**
     * 编制入口场景的占位同步实现，返回固定项目与标段结构。
     */
    @Override
    public TenderDocumentBasicInfoSyncResult syncBasicInfo(TenderDocumentEntryRequest request, TenderDocumentUserContext userContext) {
        TenderDocumentBasicInfoSyncResult result = new TenderDocumentBasicInfoSyncResult();
        result.setProjectType(InteractionProjectType.PUBLIC);
        result.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        result.setProjectCode(request.getProjectId());
        result.setProjectName("示例项目");
        result.setPurchaseMethod("公开招标");
        result.setEvalMethod(InteractionEvalMethod.COMPREHENSIVE_SCORE);
        result.setProjectInfo(buildProjectInfo(request.getProjectId(), "示例项目", "示例采购单位", "2026-06-01 10:00:00", "公开招标", result.getProjectType(), result.getCompileScope()));
        result.getTenderList().add(buildTenderInfo(request.getTenderId(), 1, "示例标段", DEFAULT_TENDER_AMOUNT, "示例采购内容"));
        return result;
    }

    /**
     * 已有编制单场景的占位同步实现，尽量复用编制单已持久化信息回填结果。
     */
    @Override
    public TenderDocumentBasicInfoSyncResult syncBasicInfo(TenderDocument tenderDocument, TenderDocumentUserContext userContext) {
        TenderDocumentBasicInfoSyncResult result = new TenderDocumentBasicInfoSyncResult();
        result.setProjectType(toProjectType(tenderDocument.getCompileScope(), tenderDocument.getPurchaseMethod()));
        result.setCompileScope(tenderDocument.getCompileScope() == null ? TenderDocumentScopeType.PROJECT.name() : tenderDocument.getCompileScope());
        result.setProjectCode(tenderDocument.getProjectCode());
        result.setProjectName(tenderDocument.getProjectName());
        result.setPurchaseMethod(tenderDocument.getPurchaseMethod());
        result.setEvalMethod(toEvalMethod(tenderDocument.getEvalMethod()));
        result.setProjectInfo(buildProjectInfo(
                tenderDocument.getProjectId(),
                tenderDocument.getProjectName(),
                "示例采购单位",
                "2026-06-01 10:00:00",
                tenderDocument.getPurchaseMethod(),
                result.getProjectType(),
                result.getCompileScope()
        ));
        if (tenderDocument.getTenderId() != null) {
            result.getTenderList().add(buildTenderInfo(tenderDocument.getTenderId(), tenderDocument.getIndexOf(), "当前标段", DEFAULT_TENDER_AMOUNT, "示例采购内容"));
        }
        return result;
    }

    /**
     * 开标标录占位同步实现，返回可驱动页面展示的基础结构。
     */
    @Override
    public TenderDocumentBidRecordSyncResult syncBidRecord(TenderDocument tenderDocument, TenderDocumentUserContext userContext) {
        TenderDocumentBidRecordSyncResult result = new TenderDocumentBidRecordSyncResult();
        result.setProjectInfo(buildProjectInfo(
                tenderDocument.getProjectId(),
                tenderDocument.getProjectName(),
                "示例采购单位",
                "2026-06-01 10:00:00",
                tenderDocument.getPurchaseMethod(),
                toProjectType(tenderDocument.getCompileScope(), tenderDocument.getPurchaseMethod()),
                tenderDocument.getCompileScope()
        ));
        if (tenderDocument.getTenderId() != null) {
            result.getTenderList().add(buildTenderInfo(tenderDocument.getTenderId(), tenderDocument.getIndexOf(), "当前标段", DEFAULT_TENDER_AMOUNT, "示例采购内容"));
        } else {
            result.setTenderList(new ArrayList<>());
        }
        Map<String, Object> bidRecord = new LinkedHashMap<>();
        bidRecord.put("projectId", tenderDocument.getProjectId());
        bidRecord.put("currentStepCode", tenderDocument.getCurrentStepCode());
        // 占位实现里直接记录当前操作人，正式对接后由业务系统返回真实标录主体。
        bidRecord.put("operatorUserId", userContext.getUserId());
        result.setBidRecord(bidRecord);
        return result;
    }

    private Map<String, Object> buildProjectInfo(String projectId,
                                                 String projectName,
                                                 String purchaserName,
                                                 String bidEndTime,
                                                 String purchaseMethod,
                                                 InteractionProjectType projectType,
                                                 String compileScope) {
        Map<String, Object> projectInfo = new LinkedHashMap<>();
        projectInfo.put("projectId", projectId);
        projectInfo.put("projectName", projectName);
        projectInfo.put("purchaserName", purchaserName);
        projectInfo.put("bidEndTime", bidEndTime);
        projectInfo.put("purchaseMethod", purchaseMethod);
        projectInfo.put("projectType", projectType == null ? null : projectType.name());
        projectInfo.put("compileScope", compileScope);
        return projectInfo;
    }

    private Map<String, Object> buildTenderInfo(String tenderId, Integer indexOf, String tenderName, BigDecimal tenderAmount, String purchaseContext) {
        Map<String, Object> tenderInfo = new LinkedHashMap<>();
        tenderInfo.put("tenderId", tenderId);
        tenderInfo.put("indexOf", indexOf);
        if (indexOf != null) {
            tenderInfo.put("indexOfDesc", TenderListDisplaySupport.getIndexOfDesc(indexOf));
        }
        tenderInfo.put("tenderName", tenderName);
        tenderInfo.put("tenderAmount", tenderAmount);
        tenderInfo.put("purchaseContext", purchaseContext);
        return tenderInfo;
    }

    private InteractionProjectType toProjectType(String compileScope, String purchaseMethod) {
        if (TenderDocumentScopeType.TENDER.name().equalsIgnoreCase(compileScope)) {
            return InteractionProjectType.INVITE;
        }
        if (TenderDocumentScopeType.PROJECT.name().equalsIgnoreCase(compileScope)) {
            return InteractionProjectType.PUBLIC;
        }
        if (purchaseMethod != null && purchaseMethod.contains("邀请")) {
            return InteractionProjectType.INVITE;
        }
        return InteractionProjectType.PUBLIC;
    }

    private InteractionEvalMethod toEvalMethod(String evalMethod) {
        return InteractionEvalMethod.COMPREHENSIVE_SCORE.name().equalsIgnoreCase(evalMethod)
                ? InteractionEvalMethod.COMPREHENSIVE_SCORE
                : InteractionEvalMethod.LOWEST_PRICE;
    }
}
