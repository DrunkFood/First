package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.request.ChatRequest;
import com.jy.eleaitender.ai.dto.request.OptimizeRequest;
import com.jy.eleaitender.ai.model.ModelRouter;
import com.jy.eleaitender.ai.prompt.PromptBuilder;
import com.jy.eleaitender.ai.prompt.PromptTemplates;
import com.jy.eleaitender.ai.recorder.AiCallRecorder;
import com.jy.eleaitender.ai.service.IAiChatService;
import com.jy.eleaitender.common.enums.AiUsageScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * AI对话服务实现
 * 支持SSE流式响应和同步调用
 */
@Slf4j
@Service
public class AiChatServiceImpl implements IAiChatService {

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void streamChat(ChatRequest request, SseEmitter emitter) {
        executor.execute(() -> {
            try {
                ChatClient chatClient = modelRouter.route(AiUsageScenario.OPTIMIZATION);

                // 构建对话历史
                List<Message> chatMessages = new ArrayList<>();
                if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                    for (ChatRequest.ChatMessage histMsg : request.getHistory()) {
                        if ("user".equals(histMsg.getRole())) {
                            chatMessages.add(new UserMessage(histMsg.getContent()));
                        } else if ("assistant".equals(histMsg.getRole())) {
                            chatMessages.add(new AssistantMessage(histMsg.getContent()));
                        }
                    }
                }

                // 构建用户消息
                StringBuilder userPrompt = new StringBuilder();
                if (request.getContext() != null && !request.getContext().isBlank()) {
                    userPrompt.append("参考上下文：\n").append(request.getContext()).append("\n\n");
                }
                userPrompt.append(request.getMessage());

                // 流式调用 - 使用chatResponse以获取token信息
                StringBuilder contentBuilder = new StringBuilder();
                AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();

                Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                        .system(PromptTemplates.AI_ASSISTANT)
                        .messages(chatMessages)
                        .user(userPrompt.toString())
                        .stream()
                        .chatResponse();

                chatResponseFlux.subscribe(
                        chatResponse -> {
                            String chunk = extractChunk(chatResponse);
                            if (chunk != null && !chunk.isEmpty()) {
                                contentBuilder.append(chunk);
                                sendSseEvent(emitter, chunk);
                            }
                            lastResponseRef.set(chatResponse);
                        },
                        error -> completeSseWithError(emitter, error),
                        () -> {
                            // 流完成后记录响应日志
                            aiCallRecorder.recordStreamResponse(
                                    lastResponseRef.get(), contentBuilder.toString(),
                                    PromptTemplates.AI_ASSISTANT, userPrompt.toString(),
                                    "CHAT", null, request.getConversationId(), null);
                            completeSse(emitter);
                        }
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

                // 流式调用 - 使用chatResponse以获取token信息
                StringBuilder contentBuilder = new StringBuilder();
                AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();

                Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                        .system(PromptTemplates.TEXT_OPTIMIZE)
                        .user(userPrompt)
                        .stream()
                        .chatResponse();

                chatResponseFlux.subscribe(
                        chatResponse -> {
                            String chunk = extractChunk(chatResponse);
                            if (chunk != null && !chunk.isEmpty()) {
                                contentBuilder.append(chunk);
                                sendSseEvent(emitter, chunk);
                            }
                            lastResponseRef.set(chatResponse);
                        },
                        error -> completeSseWithError(emitter, error),
                        () -> {
                            aiCallRecorder.recordStreamResponse(
                                    lastResponseRef.get(), contentBuilder.toString(),
                                    PromptTemplates.TEXT_OPTIMIZE, userPrompt,
                                    "OPTIMIZATION", null, null, null);
                            completeSse(emitter);
                        }
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

        return aiCallRecorder.callAndRecord(chatClient, PromptTemplates.AI_ASSISTANT,
                userPrompt.toString(), "CHAT", null, null, null);
    }

    /**
     * 从流式ChatResponse中提取增量文本
     */
    private String extractChunk(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return null;
        }
        return chatResponse.getResult().getOutput().getContent();
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
