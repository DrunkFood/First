package com.jy.eleaitender.interaction.autoconfigure.controller;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import com.jy.eleaitender.common.interaction.dto.ExternalUserInfoResponse;
import com.jy.eleaitender.common.interaction.dto.IdentityContext;
import com.jy.eleaitender.common.interaction.dto.IdentityQueryResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.common.interaction.spi.InteractionIdentityService;
import com.jy.eleaitender.interaction.core.client.EleTenderInteractionClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身份查询控制器
 */
@RestController
public class InteractionIdentityController {

    private final EleTenderInteractionClient interactionClient;
    private final InteractionIdentityService identityService;
    private final InteractionEventLogger eventLogger;

    public InteractionIdentityController(EleTenderInteractionClient interactionClient,
                                         InteractionIdentityService identityService,
                                         InteractionEventLogger eventLogger) {
        this.interactionClient = interactionClient;
        this.identityService = identityService;
        this.eventLogger = eventLogger;
    }

    /**
     * 查询当前外部用户在业务系统中的身份扩展信息。
     * 先通过电子标 token 还原外部用户，再转成业务系统可识别的 IdentityContext。
     */
    @GetMapping(value = InteractionApiPaths.IDENTITY_CURRENT, produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<IdentityQueryResponse> queryCurrentIdentity(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Throwable error = null;
        IdentityContext context = null;
        InteractionResult<IdentityQueryResponse> result = null;
        try {
            ExternalUserInfoResponse externalUser = interactionClient.getCurrentExternalUser(authorization);
            context = IdentityContext.from(externalUser);
            result = InteractionResult.success(identityService.queryCurrentIdentity(context));
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("identity/current", context, result, error);
            }
        }
    }
}
