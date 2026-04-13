package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eleaitender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.security.annotation.RequirePermission;
import com.jy.eleaitender.support.service.ICryptoManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto/manage")
@Tag(name = "加密系统管理")
public class CryptoManageController {

    @Autowired
    private ICryptoManageService cryptoManageService;

    @GetMapping("/requests")
    @RequireLogin
    @RequirePermission("crypto-request:view")
    @Operation(summary = "分页查询解密请求")
    public Result<Page<BdcDecryptRequest>> getRequestPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) String appKey,
                                                          @RequestParam(required = false) String projectId,
                                                          @RequestParam(required = false) String tenderId,
                                                          @RequestParam(required = false) String bidRecordId,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String callbackStatus) {
        return Result.success(cryptoManageService.getRequestPage(
                pageNum,
                pageSize,
                appKey,
                projectId,
                tenderId,
                bidRecordId,
                status,
                callbackStatus));
    }

    @GetMapping("/artifacts/{artifactId}")
    @RequireLogin
    @RequirePermission("crypto-artifact:view")
    @Operation(summary = "查询解密工件明细")
    public Result<BdcDecryptArtifact> getArtifactDetail(@PathVariable("artifactId") Long artifactId) {
        return Result.success(cryptoManageService.getArtifactDetail(artifactId));
    }

    @PostMapping("/requests/{requestId}/retry")
    @RequireLogin
    @RequirePermission("crypto-request:retry")
    @Operation(summary = "人工重试解密请求")
    public Result<Boolean> retryRequest(@PathVariable("requestId") Long requestId) {
        cryptoManageService.retryRequest(requestId);
        return Result.success(Boolean.TRUE);
    }
}
