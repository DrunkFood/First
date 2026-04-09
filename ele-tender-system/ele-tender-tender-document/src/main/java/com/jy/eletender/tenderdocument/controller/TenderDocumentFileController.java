package com.jy.eletender.tenderdocument.controller;

import com.jy.eletender.common.response.Result;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentFileBindRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFilePageResponse;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.tenderdocument.service.ITenderDocumentFileService;
import com.jy.eletender.tenderdocument.support.CurrentExternalUserResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 采购文件与签章文件控制器。
 * 负责采购文件环节的查询、绑定和解绑，不直接操作底层文件物理存储。
 */
@RestController
@RequireLogin
@RequestMapping("/api/tender-documents")
public class TenderDocumentFileController {

    private final ITenderDocumentFileService tenderDocumentFileService;
    private final CurrentExternalUserResolver currentExternalUserResolver;

    public TenderDocumentFileController(ITenderDocumentFileService tenderDocumentFileService,
                                        CurrentExternalUserResolver currentExternalUserResolver) {
        this.tenderDocumentFileService = tenderDocumentFileService;
        this.currentExternalUserResolver = currentExternalUserResolver;
    }

    /**
     * 获取采购文件编制页数据。
     * 页面只关心当前编制单下已绑定的文件展示结果，因此直接返回 fileList。
     */
    @GetMapping("/purchase-file")
    public Result<TenderDocumentFilePageResponse> getPurchaseFilePage(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentFileService.getPurchaseFilePage(tenderDocumentId, userContext));
    }

    /**
     * 绑定采购文件。
     * 前端需先把文件上传到文件服务，再把 fileId 和关联范围传给编制系统完成绑定。
     */
    @PostMapping("/purchase-file/bind")
    public Result<Void> bindPurchaseFile(@RequestParam Long tenderDocumentId,
                                         @Valid @RequestBody TenderDocumentFileBindRequest request) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentFileService.bindPurchaseFile(tenderDocumentId, request, userContext);
        return Result.success();
    }

    /**
     * 解绑当前采购文件。
     * 这里只解除编制单和文件的业务关联，不直接删除文件服务里的原始文件。
     */
    @DeleteMapping("/purchase-file")
    public Result<Void> removePurchaseFile(@RequestParam Long tenderDocumentId,
                                           @RequestParam(required = false) String tenderId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentFileService.removePurchaseFile(tenderDocumentId, tenderId, userContext);
        return Result.success();
    }

    /**
     * 获取采购文件环节下的签章文件列表。
     */
    @GetMapping("/purchase-file/signed")
    public Result<TenderDocumentFilePageResponse> getSignedFilePage(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentFileService.getSignedFilePage(tenderDocumentId, userContext));
    }

    /**
     * 绑定采购文件环节上传的签章文件。
     */
    @PostMapping("/purchase-file/signed/bind")
    public Result<Void> bindSignedFile(@RequestParam Long tenderDocumentId,
                                       @Valid @RequestBody TenderDocumentFileBindRequest request) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentFileService.bindSignedFile(tenderDocumentId, request, userContext);
        return Result.success();
    }

    /**
     * 解绑采购文件环节中的签章文件。
     */
    @DeleteMapping("/purchase-file/signed")
    public Result<Void> removeSignedFile(@RequestParam Long tenderDocumentId,
                                         @RequestParam(required = false) String tenderId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentFileService.removeSignedFile(tenderDocumentId, tenderId, userContext);
        return Result.success();
    }
}
