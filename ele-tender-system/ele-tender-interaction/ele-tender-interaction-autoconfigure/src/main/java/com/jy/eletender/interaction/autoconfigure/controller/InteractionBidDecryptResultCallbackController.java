package com.jy.eletender.interaction.autoconfigure.controller;

import com.jy.eletender.common.interaction.constant.InteractionApiPaths;
import com.jy.eletender.common.interaction.dto.BidDecryptResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.spi.InteractionBidDecryptResultReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionEventLogger;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投标文件解密结果回调控制器
 */
@RestController
public class InteractionBidDecryptResultCallbackController {

    private final InteractionBidDecryptResultReceiveService receiveService;
    private final InteractionEventLogger eventLogger;

    public InteractionBidDecryptResultCallbackController(InteractionBidDecryptResultReceiveService receiveService,
                                                         InteractionEventLogger eventLogger) {
        this.receiveService = receiveService;
        this.eventLogger = eventLogger;
    }

    @PostMapping(value = InteractionApiPaths.CALLBACK_BID_DECRYPT_RESULT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<Void> onDecryptResult(@RequestBody BidDecryptResultCallbackRequest request) {
        Throwable error = null;
        InteractionResult<Void> result = null;
        try {
            InteractionValidationUtils.validateBidDecryptResultCallbackRequest(request);
            receiveService.onDecryptResult(request);
            result = InteractionResult.success();
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("callbacks/bid-decrypt-result", request, result, error);
            }
        }
    }
}
