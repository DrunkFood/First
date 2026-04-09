package com.jy.eletender.tenderdocument.controller;

import com.jy.eletender.common.response.Result;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEntryResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentOverviewResponse;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.tenderdocument.service.ITenderDocumentService;
import com.jy.eletender.tenderdocument.support.CurrentExternalUserResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 编制单入口与概览查询控制器。
 * 负责接收外部系统上下文并转交服务层处理编制单生命周期入口能力。
 */
@RestController
@RequireLogin
@RequestMapping("/api/tender-documents")
public class TenderDocumentController {

    private final ITenderDocumentService tenderDocumentService;
    private final CurrentExternalUserResolver currentExternalUserResolver;

    public TenderDocumentController(ITenderDocumentService tenderDocumentService,
                                    CurrentExternalUserResolver currentExternalUserResolver) {
        this.tenderDocumentService = tenderDocumentService;
        this.currentExternalUserResolver = currentExternalUserResolver;
    }

    /**
     * 编制入口。
     * 先解析当前业务系统跳转进来的外部用户，再由服务层统一完成项目锁校验、
     * 基本信息同步以及编制单命中/创建。
     */
    @PostMapping("/entry")
    public Result<TenderDocumentEntryResponse> enter(@Valid @RequestBody TenderDocumentEntryRequest request) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentService.enter(request, userContext));
    }

    /**
     * 获取编制概览。
     * 该接口只返回页面壳需要的全局信息，不直接暴露内部步骤快照明细。
     */
    @GetMapping("/overview")
    public Result<TenderDocumentOverviewResponse> getOverview(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentService.getOverview(tenderDocumentId, userContext));
    }

    /**
     * 重新编制。
     * 将已完成的编制单原地重置为草稿状态，版本号递增，前端可重新触发生成。
     */
    @PostMapping("/recompile")
    public Result<TenderDocumentEntryResponse> recompile(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentService.recompile(tenderDocumentId, userContext));
    }
}
