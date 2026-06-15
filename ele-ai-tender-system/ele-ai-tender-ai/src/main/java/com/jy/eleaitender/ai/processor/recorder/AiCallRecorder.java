package com.jy.eleaitender.ai.processor.recorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.service.FileContentService;
import com.jy.eleaitender.ai.service.IAiResponseLogService;
import com.jy.eleaitender.common.entity.ai.AiResponseLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * AI调用记录器
 * 封装ChatClient调用，自动记录响应详情（模型、tokens、内容等）
 */
@Slf4j
@Component
public class AiCallRecorder {

    @Autowired
    private IAiResponseLogService responseLogService;

    @Autowired
    private FileContentService fileContentService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 同步调用AI并记录响应日志
     * 替代 client.prompt()...call().content()
     *
     * @param client       ChatClient实例
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param role         对话角色: GENERATION/OPTIMIZATION/DETECTION/CHAT
     * @param taskId       关联AI任务ID（对话类调用传null）
     * @param userId       用户ID（任务类调用取task.createId，对话类传null由MetaObjectHandler填充）
     * @param fileIds      关联文件ID列表
     * @return AI响应内容
     */
    public String callAndRecord(ChatClient client, String systemPrompt, String userPrompt,
                                String role, Long taskId, Long userId, List<String> fileIds) {
        String fileContents = fileContentService.resolveFileContents(fileIds);
        userPrompt = userPrompt + fileContents;

        // 记录开始时间
        Date startTime = new Date();

        ChatResponse chatResponse = client.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse();

        String content = extractContent(chatResponse);
        record(chatResponse, content, systemPrompt, userPrompt, startTime, role, taskId, null, userId);
        return content;
    }

    /**
     * 记录流式响应日志（流完成后调用）
     *
     * @param lastChatResponse 流的最后一个ChatResponse（包含usage信息）
     * @param fullContent      流式收集的完整内容
     * @param systemPrompt     系统提示词
     * @param userPrompt       用户提示词
     * @param role             对话角色
     * @param taskId           关联AI任务ID
     * @param conversationId   对话ID
     * @param userId           用户ID
     */
    public void recordStreamResponse(ChatResponse lastChatResponse, String fullContent,
                                     String systemPrompt, String userPrompt,
                                     String role, Long taskId, String conversationId, Long userId) {
        record(lastChatResponse, fullContent, systemPrompt, userPrompt, new Date(), role, taskId, conversationId, userId);
    }

    private void record(ChatResponse chatResponse, String content,
                        String systemPrompt, String userPrompt, Date startTime,
                        String role, Long taskId, String conversationId, Long userId) {
        try {
            AiResponseLog responseLog = new AiResponseLog();
            responseLog.setModel(getModel(chatResponse));
            responseLog.setRole(role);
            responseLog.setMessages(buildMessagesJson(systemPrompt, userPrompt));
            responseLog.setContent(content);
            responseLog.setFinishReason(getFinishReason(chatResponse));
            responseLog.setPromptTokens(getPromptTokens(chatResponse));
            responseLog.setCompletionTokens(getCompletionTokens(chatResponse));
            responseLog.setTotalTokens(getTotalTokens(chatResponse));
            responseLog.setTaskId(taskId);
            responseLog.setConversationId(conversationId);
            // 任务类调用显式设置用户ID，对话类由MetaObjectHandler自动填充
            if (userId != null) {
                responseLog.setCreateId(userId);
            }
            responseLog.setCreateTime(startTime);
            responseLog.setModifyTime(new Date());
            responseLogService.record(responseLog);
        } catch (Exception e) {
            log.error("记录AI响应日志失败: role={}, taskId={}", role, taskId, e);
        }
    }

    // ========== ChatResponse 信息提取 ==========

    private String extractContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getContent();
    }

    private String getModel(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return "";
        }
        // ChatResponseMetadata 继承 Map<String, Object>，模型名称以 key="model" 存储
        Object model = chatResponse.getMetadata().get("model");
        return model != null ? model.toString() : "";
    }

    private String getFinishReason(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getMetadata() == null) {
            return "";
        }
        String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
        return finishReason != null ? finishReason : "";
    }

    private int getPromptTokens(ChatResponse chatResponse) {
        Usage usage = getUsage(chatResponse);
        return usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0;
    }

    private int getCompletionTokens(ChatResponse chatResponse) {
        Usage usage = getUsage(chatResponse);
        return usage != null && usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : 0;
    }

    private int getTotalTokens(ChatResponse chatResponse) {
        Usage usage = getUsage(chatResponse);
        return usage != null && usage.getTotalTokens() != null ? usage.getTotalTokens().intValue() : 0;
    }

    private Usage getUsage(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return null;
        }
        return chatResponse.getMetadata().getUsage();
    }

    // ========== Messages JSON 构建 ==========

    private String buildMessagesJson(String systemPrompt, String userPrompt) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            if (userPrompt != null && !userPrompt.isBlank()) {
                messages.add(Map.of("role", "user", "content", userPrompt));
            }
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            log.warn("构建messages JSON失败", e);
            return "[]";
        }
    }
}
