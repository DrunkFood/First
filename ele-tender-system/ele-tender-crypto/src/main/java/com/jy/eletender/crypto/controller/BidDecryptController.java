package com.jy.eletender.crypto.controller;

import com.jy.eletender.common.interaction.dto.BidDecryptStatusResponse;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.crypto.service.IBidDecryptRequestService;
import com.jy.eletender.crypto.support.CurrentExternalUserResolver;
import com.jy.eletender.crypto.support.CryptoUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto")
@Tag(name = "投标文件加解密")
/**
 * 解密请求入口。
 * 该入口对应 `bdc_decrypt_request` 的创建起点，并通过 service 间接驱动
 * `bdc_decrypt_artifact` 的复用、异步解密和回调状态投影。
 */
public class BidDecryptController {

    private final IBidDecryptRequestService bidDecryptRequestService;
    private final CurrentExternalUserResolver currentExternalUserResolver;

    public BidDecryptController(IBidDecryptRequestService bidDecryptRequestService,
                                CurrentExternalUserResolver currentExternalUserResolver) {
        this.bidDecryptRequestService = bidDecryptRequestService;
        this.currentExternalUserResolver = currentExternalUserResolver;
    }

    @PostMapping("/bid-decrypt/submit")
    @RequireLogin
    @Operation(summary = "提交解密请求")
    public Result<BidDecryptSubmitResponse> submit(@RequestBody BidDecryptSubmitRequest request) {
        // 解密请求必须绑定当前外部用户身份，避免跨 appKey 查询或提交。
        InteractionValidationUtils.validateBidDecryptSubmitRequest(request);
        CryptoUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(bidDecryptRequestService.submit(userContext, request));
    }

    @GetMapping("/bid-decrypt/status/{recordId}")
    @RequireLogin
    @Operation(summary = "查询解密状态")
    public Result<BidDecryptStatusResponse> queryStatus(@PathVariable("recordId") String recordId) {
        // 查询时使用当前 appKey 做边界校验，防止读取其他业务系统的 recordId。
        CryptoUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(bidDecryptRequestService.queryStatus(userContext.getAppKey(), recordId));
    }
}
