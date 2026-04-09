package com.jy.eletender.tenderdocument.controller;

import com.jy.eletender.common.response.Result;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentCallbackRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGeneratePageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerateResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerationRecordView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentUnifiedCallbackResponse;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.tenderdocument.service.ITenderDocumentGenerationService;
import com.jy.eletender.tenderdocument.support.CurrentExternalUserResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 编制产物生成与回传控制器。
 * 对外提供生成页查询、正式生成及统一回传入口。
 */
@RestController
@RequireLogin
@RequestMapping("/api/tender-documents")
public class TenderDocumentGenerationController {

    private final ITenderDocumentGenerationService tenderDocumentGenerationService;
    private final CurrentExternalUserResolver currentExternalUserResolver;

    public TenderDocumentGenerationController(ITenderDocumentGenerationService tenderDocumentGenerationService,
                                              CurrentExternalUserResolver currentExternalUserResolver) {
        this.tenderDocumentGenerationService = tenderDocumentGenerationService;
        this.currentExternalUserResolver = currentExternalUserResolver;
    }

    /**
     * 获取生成文件页数据。
     * 页面需要同时展示签章文件、数据包、编制信息和回传历史，因此统一由此接口聚合返回。
     */
    @GetMapping("/generate")
    public Result<TenderDocumentGeneratePageResponse> getGeneratePage(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentGenerationService.getGeneratePage(tenderDocumentId, userContext));
    }

    /**
     * 查询生成记录列表，按时间倒序返回，供页面查看“生成中/成功/失败”历史。
     */
    @GetMapping("/generate/records")
    public Result<List<TenderDocumentGenerationRecordView>> listGenerateRecords(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentGenerationService.listGenerateRecords(tenderDocumentId, userContext));
    }

    /**
     * 生成采购文件数据包、编制信息等最终产物。
     * 成功后编制单会进入 COMPLETED 状态，同时固化完成版本。
     * 注意：该接口只负责“生成”，不会触发回传；回传需单独调用 /generate/callback。
     */
    @PostMapping("/generate")
    public Result<TenderDocumentGenerateResponse> generate(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentGenerationService.generate(tenderDocumentId, userContext));
    }

    /**
     * 统一回传入口。
     * 前端只感知一个“回传文件”按钮，后端内部再分别调用签章文件和数据包回传能力。
     */
    @PostMapping("/generate/callback")
    public Result<TenderDocumentUnifiedCallbackResponse> callbackAll(@RequestParam Long tenderDocumentId,
                                                                     @RequestBody(required = false) TenderDocumentCallbackRequest request) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        TenderDocumentUnifiedCallbackResponse response = tenderDocumentGenerationService.callbackAll(
                tenderDocumentId,
                // 统一回传接口允许前端不传请求体，此时按默认空对象处理重试参数。
                request == null ? new TenderDocumentCallbackRequest() : request,
                userContext
        );
        if (Boolean.TRUE.equals(response.getSuccess())) {
            return Result.success(response);
        }
        return Result.fail(response.getMessage());
    }
}
