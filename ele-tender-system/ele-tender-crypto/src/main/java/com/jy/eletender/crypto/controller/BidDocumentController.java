package com.jy.eletender.crypto.controller;

import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushResponse;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.crypto.service.IBidDocumentService;
import com.jy.eletender.crypto.support.CurrentExternalUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto")
@Tag(name = "投标文件加解密")
/**
 * 投标文件预存入口。
 * 该入口对应 `bdc_bid_document` 的写入起点，完成参数校验后把当前外部用户上下文
 * 传给 service，由 service 负责文件下载、落盘、持久化和回调。
 */
public class BidDocumentController {

    private final IBidDocumentService bidDocumentService;
    private final CurrentExternalUserResolver currentExternalUserResolver;

    public BidDocumentController(IBidDocumentService bidDocumentService,
                             CurrentExternalUserResolver currentExternalUserResolver) {
        this.bidDocumentService = bidDocumentService;
        this.currentExternalUserResolver = currentExternalUserResolver;
    }

    @PostMapping("/bid-document/push")
    @RequireLogin
    @Operation(summary = "推送投标文件预存")
    public Result<BidDocumentPushResponse> pushBidDocument(@RequestBody BidDocumentPushRequest request) {
        // controller 只做协议校验与身份解析，不在入口层处理落盘和回调细节。
        InteractionValidationUtils.validateBidDocumentPushRequest(request);
        return Result.success(bidDocumentService.pushBidDocument(currentExternalUserResolver.resolve(), request));
    }
}
