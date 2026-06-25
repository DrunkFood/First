package com.jy.eleaitender.ai.processor.recorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.service.FileContentService;
import com.jy.eleaitender.ai.service.IAiResponseLogService;
import com.jy.eleaitender.common.entity.ai.AiResponseLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 调用记录器。
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

    public String callAndRecord(ChatClient client, String systemPrompt, String userPrompt,
                                String role, Long taskId, Long userId, List<String> fileIds) {
        return callAndRecord(client, systemPrompt, userPrompt, role, taskId, userId, fileIds, null);
    }

    public String callAndRecord(ChatClient client, String systemPrompt, String userPrompt,
                                String role, Long taskId, Long userId, List<String> fileIds,
                                String modelName) {
        String resolvedUserPrompt = buildUserPromptWithFiles(userPrompt, fileIds);
        List<Message> messages = buildChatMessages(systemPrompt, resolvedUserPrompt);
        Date startTime = new Date();

        ChatResponse chatResponse = client.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        String content = extractContent(chatResponse);
        record(chatResponse, content, systemPrompt, resolvedUserPrompt, startTime, role, taskId, null, userId, modelName);
        return content;
    }

    public String callAndRecord(ChatClient client, List<Message> messages,
                                String role, Long taskId, Long userId, List<String> fileIds,
                                String modelName) {
        String fileContents = fileContentService.resolveFileContents(fileIds);
        if (StringUtils.hasText(fileContents)) {
            messages.add(new UserMessage(fileContents));
        }
        Date startTime = new Date();

        ChatResponse chatResponse = client.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        String content = extractContent(chatResponse);
        record(chatResponse, content, messages, startTime, role, taskId, null, userId, modelName);
        return content;
    }

    private List<Message> buildChatMessages(String systemPrompt, String userPrompt) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (StringUtils.hasText(userPrompt)) {
            messages.add(new UserMessage(userPrompt));
        }
        return messages;
    }

    public String buildUserPromptWithFiles(String userPrompt, List<String> fileIds) {
        String fileContents = fileContentService.resolveFileContents(fileIds);
        return (userPrompt == null ? "" : userPrompt) + fileContents;
    }

    public void record(ChatResponse chatResponse, String content,
                       List<Message> messages, Date startTime,
                       String role, Long taskId, String conversationId, Long userId, String modelName) {
        String systemPrompt = messages.stream()
                .filter(message -> message instanceof SystemMessage)
                .map(Content::getContent)
                .collect(Collectors.joining("\n"));
        String userPrompt = messages.stream()
                .filter(message -> message instanceof UserMessage)
                .map(Content::getContent)
                .collect(Collectors.joining("\n"));
        record(chatResponse, content, systemPrompt, userPrompt, startTime, role, taskId, conversationId, userId, modelName);
    }

    public void record(ChatResponse chatResponse, String content,
                       String systemPrompt, String userPrompt, Date startTime,
                       String role, Long taskId, String conversationId, Long userId, String modelName) {
        try {
            AiResponseLog responseLog = new AiResponseLog();
            responseLog.setModel(getModel(chatResponse, modelName));
            responseLog.setRole(role);
            responseLog.setMessages(buildMessagesJson(systemPrompt, userPrompt));
            responseLog.setContent(content);
            responseLog.setFinishReason(getFinishReason(chatResponse));
            responseLog.setPromptTokens(getPromptTokens(chatResponse));
            responseLog.setCompletionTokens(getCompletionTokens(chatResponse));
            responseLog.setTotalTokens(getTotalTokens(chatResponse));
            responseLog.setTaskId(taskId);
            responseLog.setConversationId(conversationId);
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

    private String extractContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getContent();
    }

    private String getModel(ChatResponse chatResponse, String fallbackModelName) {
        String responseModel = "";
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            Object model = chatResponse.getMetadata().get("model");
            responseModel = model != null ? model.toString() : "";
        }
        if (StringUtils.hasText(responseModel)) {
            return responseModel;
        }
        return StringUtils.hasText(fallbackModelName) ? fallbackModelName : "";
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

    private String buildMessagesJson(String systemPrompt, String userPrompt) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (StringUtils.hasText(systemPrompt)) {
                messages.add(Map.of("role", MessageType.SYSTEM.getValue(), "content", systemPrompt));
            }
            if (StringUtils.hasText(userPrompt)) {
                messages.add(Map.of("role", MessageType.USER.getValue(), "content", userPrompt));
            }
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            log.warn("构建messages JSON失败", e);
            return "[]";
        }
    }
}
