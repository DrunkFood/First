package com.jy.eletender.crypto.controller;

import com.jy.eletender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eletender.common.interaction.dto.EnvelopeJoinResponse;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.crypto.service.IEnvelopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto")
@Tag(name = "密钥信封")
public class EnvelopeController {

    private final IEnvelopeService envelopeService;

    public EnvelopeController(IEnvelopeService envelopeService) {
        this.envelopeService = envelopeService;
    }

    @PostMapping("/envelope/join")
    @RequireLogin
    @Operation(summary = "拼接信封还原 bidderPwdStr")
    public Result<EnvelopeJoinResponse> join(@RequestBody EnvelopeJoinRequest request) {
        InteractionValidationUtils.validateEnvelopeJoinRequest(request);
        return Result.success(envelopeService.join(request));
    }
}
