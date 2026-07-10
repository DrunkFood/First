package com.jy.eleaitender.interaction.autoconfigure.controller;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import com.jy.eleaitender.common.interaction.dto.AiTaskResultCallbackRequest;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.spi.InteractionAiTaskResultReceiveService;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI任务结果回调控制器
 */
@RestController
public class InteractionAiTaskResultCallbackController {

    private final InteractionAiTaskResultReceiveService receiveService;
    private final InteractionEventLogger eventLogger;

    public InteractionAiTaskResultCallbackController(InteractionAiTaskResultReceiveService receiveService,
                                                      InteractionEventLogger eventLogger) {
        this.receiveService = receiveService;
        this.eventLogger = eventLogger;
    }

    /**
     * 接收AI任务终态结果回调。
     */
    @PostMapping(value = InteractionApiPaths.CALLBACK_AI_TASK_RESULT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<Void> receiveAiTaskResult(@RequestBody AiTaskResultCallbackRequest request) {
        Throwable error = null;
        InteractionResult<Void> result = null;
        try {
            InteractionValidationUtils.validateReceiveAiTaskResult(request);
            receiveService.receive(request);
            result = InteractionResult.success();
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("callbacks/ai-task-result", request, result, error);
            }
        }
    }
}
