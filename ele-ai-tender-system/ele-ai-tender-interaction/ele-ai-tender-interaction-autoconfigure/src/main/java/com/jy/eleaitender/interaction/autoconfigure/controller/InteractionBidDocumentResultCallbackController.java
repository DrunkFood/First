package com.jy.eleaitender.interaction.autoconfigure.controller;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import com.jy.eleaitender.common.interaction.dto.BidDocumentResultCallbackRequest;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.spi.InteractionBidDocumentResultReceiveService;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投标文件预存结果回调控制器
 */
@RestController
public class InteractionBidDocumentResultCallbackController {

    private final InteractionBidDocumentResultReceiveService receiveService;
    private final InteractionEventLogger eventLogger;

    public InteractionBidDocumentResultCallbackController(InteractionBidDocumentResultReceiveService receiveService,
                                                      InteractionEventLogger eventLogger) {
        this.receiveService = receiveService;
        this.eventLogger = eventLogger;
    }

    @PostMapping(value = InteractionApiPaths.CALLBACK_BID_DOCUMENT_RESULT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<Void> onBidDocumentResult(@RequestBody BidDocumentResultCallbackRequest request) {
        Throwable error = null;
        InteractionResult<Void> result = null;
        try {
            InteractionValidationUtils.validateBidDocumentResultCallbackRequest(request);
            receiveService.onBidDocumentResult(request);
            result = InteractionResult.success();
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("callbacks/bid-document-result", request, result, error);
            }
        }
    }
}
