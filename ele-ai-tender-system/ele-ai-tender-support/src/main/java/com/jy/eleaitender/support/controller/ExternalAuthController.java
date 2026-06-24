package com.jy.eleaitender.support.controller;

import com.jy.eleaitender.common.dto.response.ExternalTokenResponse;
import com.jy.eleaitender.common.dto.response.ExternalUserInfoResponse;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.AuthException;
import com.jy.eleaitender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.support.converter.ExternalTokenRequestMapper;
import com.jy.eleaitender.support.model.external.ExternalTokenIssueCommand;
import com.jy.eleaitender.support.model.external.ExternalTokenIssueResult;
import com.jy.eleaitender.support.model.external.ExternalUserInfoView;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 外部系统认证控制器
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/external", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "外部系统认证")
public class ExternalAuthController {

    @Autowired
    private IAuthService authService;

    @Autowired
    private ExternalTokenRequestMapper tokenRequestMapper;

    @PostMapping("/token")
    @Operation(summary = "外部系统用户换取Token")
    /**
     * 供业务系统通过 appKey/appSecret 签名换取电子标外部 token。
     */
    public Result<ExternalTokenResponse> getToken(
            @Parameter(description = "应用Key", required = true) @RequestHeader("X-App-Key") String appKey,
            @Parameter(description = "时间戳", required = true) @RequestHeader("X-Timestamp") Long timestamp,
            @Parameter(description = "签名", required = true) @RequestHeader("X-Signature") String signature,
            @RequestBody ExternalTokenRequest request) {
        log.info("外部系统获取Token请求: appKey={}, timestamp={}, request={}", appKey, timestamp, request);

        try {
            // 先校验请求头与请求体，再验签，避免不完整请求进入认证逻辑。
            if (StringUtils.isBlank(appKey) || timestamp == null || StringUtils.isBlank(signature)) {
                throw new AuthException(ResponseCode.PARAM_ERROR, "请求头参数不完整");
            }

            ExternalTokenIssueCommand command = tokenRequestMapper.toIssueCommand(request);

            // 验证签名
            if (!authService.verifyExternalSignature(appKey, timestamp, signature)) {
                throw new AuthException(ResponseCode.SIGNATURE_ERROR);
            }

            // 获取Token
            ExternalTokenIssueResult result = authService.getExternalToken(appKey, command);
            ExternalTokenResponse response = new ExternalTokenResponse(result.getToken(), result.getExpireIn());
            log.info("外部系统获取Token成功: appKey={}, userId={}, enterpriseId={}",
                    appKey, command.getUserId(), command.getEnterpriseId());
            return Result.success(response);
        } catch (RuntimeException e) {
            log.warn("外部系统获取Token失败: appKey={}, request={}, reason={}", appKey, request, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/verify")
    @Operation(summary = "验证外部系统签名")
    /**
     * 提供简单验签能力，便于联调时排查 appKey/appSecret 配置问题。
     */
    public Result<Boolean> verify(
            @Parameter(description = "应用Key", required = true) @RequestHeader("X-App-Key") String appKey,
            @Parameter(description = "时间戳", required = true) @RequestHeader("X-Timestamp") Long timestamp,
            @Parameter(description = "签名", required = true) @RequestHeader("X-Signature") String signature) {

        boolean valid = authService.verifyExternalSignature(appKey, timestamp, signature);
        return Result.success(valid);
    }

    @GetMapping("/userinfo")
    @RequireLogin
    @Operation(summary = "获取当前外部用户信息")
    /**
     * 当前外部 token 已通过登录校验后，返回与之对应的外部用户上下文。
     */
    public Result<ExternalUserInfoResponse> getExternalUserInfo() {
        ExternalUserInfoView view = authService.getExternalUserInfo();
        ExternalUserInfoResponse response = new ExternalUserInfoResponse();
        response.setAppKey(view.getAppKey());
        response.setUserId(view.getUserId());
        response.setUserName(view.getUserName());
        response.setEnterpriseId(view.getEnterpriseId());
        response.setEnterpriseName(view.getEnterpriseName());
        response.setEnterpriseCode(view.getEnterpriseCode());
        return Result.success(response);
    }
}
