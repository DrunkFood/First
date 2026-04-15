package com.jy.eleaitender.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.support.AiModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态ChatClient工厂
 * 根据数据库中的模型配置动态创建ChatClient实例
 * 所有模型类型(CLOUD/LOCAL/PRIVATE)均使用OpenAI兼容协议
 */
@Slf4j
@Component
public class DynamicChatClientFactory {

    /** ChatClient缓存，key=modelConfigId */
    private final ConcurrentHashMap<Long, ChatClient> clientCache = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取或创建ChatClient（带缓存）
     *
     * @param config 数据库中的模型配置
     * @return ChatClient实例
     */
    public ChatClient getOrCreateChatClient(AiModelConfig config) {
        return clientCache.computeIfAbsent(config.getId(), id -> buildChatClient(config));
    }

    /**
     * 清除所有缓存的ChatClient（配置刷新时调用）
     */
    public void evictAll() {
        clientCache.clear();
        log.info("ChatClient缓存已清除, 下次调用将重新创建");
    }

    /**
     * 根据模型配置构建ChatClient
     */
    private ChatClient buildChatClient(AiModelConfig config) {
        log.info("创建ChatClient: model={}, type={}, endpoint={}",
                config.getModelName(), config.getModelType(), config.getApiEndpoint());

        // 解析模型参数
        ModelParams params = parseModelParams(config.getModelParams());

        // 确定模型名称：优先从modelParams中获取，其次用modelName字段
        String modelName = params.model != null ? params.model : config.getModelName();

        // 创建OpenAI兼容的API客户端（适用于DeepSeek、GPT、本地vLLM/Ollama等）
        OpenAiApi api = new OpenAiApi(config.getApiEndpoint(), config.getApiKey());

        // 构建ChatOptions
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .withModel(modelName);

        if (params.temperature != null) {
            optionsBuilder.withTemperature(params.temperature.floatValue());
        }
        if (params.maxTokens != null) {
            optionsBuilder.withMaxTokens(params.maxTokens);
        }
        if (params.topP != null) {
            optionsBuilder.withTopP(params.topP.floatValue());
        }

        // 创建ChatModel
        OpenAiChatModel chatModel = new OpenAiChatModel(api, optionsBuilder.build());

        // 构建ChatClient
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 解析modelParams JSON
     */
    private ModelParams parseModelParams(String modelParamsJson) {
        ModelParams params = new ModelParams();
        if (!StringUtils.hasText(modelParamsJson)) {
            return params;
        }
        try {
            JsonNode node = objectMapper.readTree(modelParamsJson);
            if (node.has("temperature")) {
                params.temperature = node.get("temperature").doubleValue();
            }
            if (node.has("maxTokens")) {
                params.maxTokens = node.get("maxTokens").intValue();
            }
            if (node.has("topP")) {
                params.topP = node.get("topP").doubleValue();
            }
            if (node.has("model")) {
                params.model = node.get("model").asText();
            }
        } catch (Exception e) {
            log.warn("解析modelParams失败: {}", modelParamsJson, e);
        }
        return params;
    }

    /**
     * 模型参数内部类
     */
    private static class ModelParams {
        Double temperature;
        Integer maxTokens;
        Double topP;
        String model;
    }
}
