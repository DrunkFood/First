package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.request.ChatRequest;
import com.jy.eleaitender.ai.dto.request.OptimizeRequest;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.ai.service.IAiChatService;
import com.jy.eleaitender.ai.threadpool.DynamicThreadPoolManager;
import com.jy.eleaitender.common.enums.AiUsageScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI对话服务实现
 * 支持SSE流式响应和同步调用
 */
@Slf4j
@Service
public class AiChatServiceImpl implements IAiChatService {

    static final String REPLACEABLE_CONTENT_FORMAT_INSTRUCTION = """
            输出格式要求：
            如果你的回复包含可直接替换参考上下文的正文，必须使用以下固定标记包裹正文：
            【可替换正文开始】
            可直接替换到编辑器正文中的内容
            【可替换正文结束】
            标记内只放可直接替换到编辑器正文中的内容，不要放解释、修改原因、注意事项或其他说明。
            如果用户明确要求删除选中的原文，允许让标记内为空；空标记表示删除选中的原文。
            解释说明、修改原因、注意事项可以写在标记外。
            如果需要提供多个可替换正文方案，每个方案都必须单独使用一组固定标记包裹；前端会将每组标记识别为一个可选替换方案。
            请只修改“用户选中的原文”，不要改动完整 Markdown 上下文中的其他内容。
            输出的可替换正文必须尽量保持它在完整 Markdown 上下文中的结构、标题层级、编号方式、列表样式和表格列数。
            如果用户选中的原文不包含选区前的小标题、编号或冒号前缀，不要在可替换正文中补入这些前缀。
            """;

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    @Autowired
    private DynamicThreadPoolManager threadPoolManager;

    @Override
    public void streamChat(ChatRequest request, SseEmitter emitter) {
        threadPoolManager.execute(() -> {
            try {
                ChatClient chatClient = modelRouter.route(AiUsageScenario.CHAT);

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

                String userPrompt = buildUserPrompt(request);

                // 记录开始时间
                Date startTime = new Date();

                // 流式调用 - 使用chatResponse以获取token信息
                StringBuilder contentBuilder = new StringBuilder();
                AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();

                Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                        .system(SystemPromptTemplates.AI_ASSISTANT)
                        .messages(chatMessages)
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
                            // 流完成后记录响应日志
                            aiCallRecorder.record(lastResponseRef.get(), contentBuilder.toString(),
                                    SystemPromptTemplates.AI_ASSISTANT, userPrompt, startTime,
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
        threadPoolManager.execute(() -> {
            try {
                ChatClient chatClient = modelRouter.route(AiUsageScenario.OPTIMIZATION);

                String userPrompt = PromptBuilder.buildTextOptimize(
                        request.getContent(), request.getRequirement());

                // 记录开始时间
                Date startTime = new Date();

                // 流式调用 - 使用chatResponse以获取token信息
                StringBuilder contentBuilder = new StringBuilder();
                AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();

                Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                        .system(SystemPromptTemplates.TEXT_OPTIMIZE)
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
                            aiCallRecorder.record(lastResponseRef.get(), contentBuilder.toString(),
                                    SystemPromptTemplates.TEXT_OPTIMIZE, userPrompt, startTime,
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
        ChatClient chatClient = modelRouter.route(AiUsageScenario.CHAT);

        String userPrompt = buildUserPrompt(request);

        return aiCallRecorder.callAndRecord(chatClient, SystemPromptTemplates.AI_ASSISTANT,
                userPrompt, "CHAT", null, null, null);
    }

    String buildUserPrompt(ChatRequest request) {
        StringBuilder userPrompt = new StringBuilder();
        if (request.getContext() != null && !request.getContext().isBlank()) {
            if (Boolean.TRUE.equals(request.getReplaceMode())) {
                userPrompt.append("用户选中的原文：\n").append(request.getContext()).append("\n\n");
                if (request.getMarkdownContext() != null && !request.getMarkdownContext().isBlank()) {
                    userPrompt.append("完整 Markdown 上下文：\n```markdown\n")
                            .append(request.getMarkdownContext().trim())
                            .append("\n```\n\n");
                }
            } else {
                userPrompt.append("参考上下文：\n").append(request.getContext()).append("\n\n");
            }
            if (Boolean.TRUE.equals(request.getReplaceMode())) {
                userPrompt.append(REPLACEABLE_CONTENT_FORMAT_INSTRUCTION).append("\n");
            }
        }
        userPrompt.append(request.getMessage());
        return userPrompt.toString();
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
