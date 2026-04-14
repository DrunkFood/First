package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.request.ChatRequest;
import com.jy.eleaitender.ai.dto.request.OptimizeRequest;
import com.jy.eleaitender.ai.model.ModelRouter;
import com.jy.eleaitender.ai.prompt.PromptBuilder;
import com.jy.eleaitender.ai.prompt.PromptTemplates;
import com.jy.eleaitender.ai.service.IAiChatService;
import com.jy.eleaitender.common.enums.AiUsageScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI对话服务实现
 * 支持SSE流式响应和同步调用
 */
@Slf4j
@Service
public class AiChatServiceImpl implements IAiChatService {

    @Autowired
    private ModelRouter modelRouter;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void streamChat(ChatRequest request, SseEmitter emitter) {
        executor.execute(() -> {
            try {
                ChatClient chatClient = modelRouter.route(AiUsageScenario.OPTIMIZATION);

                // 构建用户消息
                StringBuilder userPrompt = new StringBuilder();
                if (request.getContext() != null && !request.getContext().isBlank()) {
                    userPrompt.append("参考上下文：\n").append(request.getContext()).append("\n\n");
                }
                userPrompt.append(request.getMessage());

                // 流式调用
                Flux<String> stream = chatClient.prompt()
                        .system(PromptTemplates.AI_ASSISTANT)
                        .user(userPrompt.toString())
                        .stream()
                        .content();

                stream.subscribe(
                        chunk -> sendSseEvent(emitter, chunk),
                        error -> completeSseWithError(emitter, error),
                        () -> completeSse(emitter)
                );
            } catch (Exception e) {
                log.error("AI对话流式调用失败", e);
                completeSseWithError(emitter, e);
            }
        });
    }

    @Override
    public void streamOptimize(OptimizeRequest request, SseEmitter emitter) {
        executor.execute(() -> {
            try {
                ChatClient chatClient = modelRouter.route(AiUsageScenario.OPTIMIZATION);

                String userPrompt = PromptBuilder.buildTextOptimize(
                        request.getContent(), request.getRequirement());

                Flux<String> stream = chatClient.prompt()
                        .system(PromptTemplates.TEXT_OPTIMIZE)
                        .user(userPrompt)
                        .stream()
                        .content();

                stream.subscribe(
                        chunk -> sendSseEvent(emitter, chunk),
                        error -> completeSseWithError(emitter, error),
                        () -> completeSse(emitter)
                );
            } catch (Exception e) {
                log.error("文本优化流式调用失败", e);
                completeSseWithError(emitter, e);
            }
        });
    }

    @Override
    public String suggest(ChatRequest request) {
        ChatClient chatClient = modelRouter.route(AiUsageScenario.OPTIMIZATION);

        StringBuilder userPrompt = new StringBuilder();
        if (request.getContext() != null && !request.getContext().isBlank()) {
            userPrompt.append("参考上下文：\n").append(request.getContext()).append("\n\n");
        }
        userPrompt.append(request.getMessage());

        return chatClient.prompt()
                .system(PromptTemplates.AI_ASSISTANT)
                .user(userPrompt.toString())
                .call()
                .content();
    }

    private void sendSseEvent(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("{\"content\":\"" + escapeJson(chunk) + "\"}"));
        } catch (IOException e) {
            log.debug("SSE发送失败(客户端可能已断开): {}", e.getMessage());
        }
    }

    private void completeSse(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data("{\"content\":\"[DONE]\"}"));
            emitter.complete();
        } catch (IOException e) {
            log.debug("SSE完成通知发送失败: {}", e.getMessage());
        }
    }

    private void completeSseWithError(SseEmitter emitter, Throwable error) {
        log.error("AI流式调用异常", error);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"" + escapeJson(error.getMessage()) + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(error);
        }
    }

    /**
     * 简单的JSON字符串转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
