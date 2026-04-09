package com.jy.eletender.tenderdocument.controller;

import com.jy.eletender.common.response.Result;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentCompleteStepRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentBasicInfoPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentBidRecordPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCheckItemsResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentOverviewResponse;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.tenderdocument.service.ITenderDocumentService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentStepService;
import com.jy.eletender.tenderdocument.support.CurrentExternalUserResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 编制步骤控制器。
 * 聚合步骤页查询、同步以及步骤推进动作，保持前端步骤条与后端状态一致。
 */
@RestController
@RequireLogin
@RequestMapping("/api/tender-documents")
public class TenderDocumentStepController {

    private final ITenderDocumentStepService tenderDocumentStepService;
    private final ITenderDocumentService tenderDocumentService;
    private final CurrentExternalUserResolver currentExternalUserResolver;

    public TenderDocumentStepController(ITenderDocumentStepService tenderDocumentStepService,
                                        ITenderDocumentService tenderDocumentService,
                                        CurrentExternalUserResolver currentExternalUserResolver) {
        this.tenderDocumentStepService = tenderDocumentStepService;
        this.tenderDocumentService = tenderDocumentService;
        this.currentExternalUserResolver = currentExternalUserResolver;
    }

    /**
     * 获取基本信息录入页数据。
     * 基本信息是页面头部的唯一来源，公开类返回项目下全部标段，邀请类只返回当前标段。
     */
    @GetMapping("/basic-info")
    public Result<TenderDocumentBasicInfoPageResponse> getBasicInfo(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentStepService.getBasicInfoPage(tenderDocumentId, userContext));
    }

    /**
     * 同步最新基本信息。
     * entry 进入逻辑与页面上的“同步最新数据”按钮都复用同一套同步能力。
     */
    @PostMapping("/basic-info/sync")
    public Result<TenderDocumentBasicInfoPageResponse> syncBasicInfo(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentStepService.syncBasicInfo(tenderDocumentId, userContext));
    }

    /**
     * 获取开标标录设置页数据。
     * 返回结构化对象供前端直接展示，不透出底层快照 JSON。
     */
    @GetMapping("/bid-record")
    public Result<TenderDocumentBidRecordPageResponse> getBidRecord(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentStepService.getBidRecordPage(tenderDocumentId, userContext));
    }

    /**
     * 同步最新标录信息。
     * 用于页面上的“同步最新数据”按钮，成功后会刷新快照和最近同步时间。
     */
    @PostMapping("/bid-record/sync")
    public Result<TenderDocumentBidRecordPageResponse> syncBidRecord(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentStepService.syncBidRecord(tenderDocumentId, userContext));
    }

    /**
     * 获取检查项页面数据。
     */
    @GetMapping("/check-items")
    public Result<TenderDocumentCheckItemsResponse> getCheckItems(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentStepService.getCheckItems(tenderDocumentId, userContext));
    }

    /**
     * 完成当前步骤并进入下一步。
     * 该接口不负责页面保存，而是在服务层完成完整校验、步骤推进和概览刷新。
     */
    @PostMapping("/complete-and-next")
    public Result<TenderDocumentOverviewResponse> completeAndNext(@RequestParam Long tenderDocumentId,
                                                                  @Valid @RequestBody TenderDocumentCompleteStepRequest request) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentStepService.completeStep(tenderDocumentId, request.getStepCode(), userContext);
        // 前端完成动作后需要立即刷新步骤条，因此这里直接返回最新概览。
        return Result.success(tenderDocumentService.getOverview(tenderDocumentId, userContext));
    }
}
