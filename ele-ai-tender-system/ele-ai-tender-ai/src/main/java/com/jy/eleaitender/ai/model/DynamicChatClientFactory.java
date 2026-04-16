package com.jy.eleaitender.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.support.AiModelConfig;
import com.jy.eleaitender.common.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 动态ChatClient工厂
 * 根据数据库中的模型配置动态创建ChatClient实例
 * 支持多种模型供应商：OPENAI兼容协议 / 智谱AI
 * 不缓存ChatClient，每次根据最新配置创建，保证配置变更实时生效
 */
@Slf4j
@Component
public class DynamicChatClientFactory {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建ChatClient（不缓存，每次根据最新配置创建）
     *
     * @param config 数据库中的模型配置
     * @return ChatClient实例
     */
    public ChatClient getOrCreateChatClient(AiModelConfig config) {
        return buildChatClient(config);
    }

    /**
     * 根据模型配置的供应商类型分发构建ChatClient
     */
    private ChatClient buildChatClient(AiModelConfig config) {
        String provider = config.getProvider();
        if ("ZHIPU".equals(provider)) {
            return buildZhiPuChatClient(config);
        }
        return buildOpenAiChatClient(config);
    }

    /**
     * 使用OpenAI兼容协议构建ChatClient
     * 适用于DeepSeek、GPT、本地vLLM/Ollama等
     */
    private ChatClient buildOpenAiChatClient(AiModelConfig config) {
        log.info("创建OpenAI兼容ChatClient: model={}, type={}, endpoint={}",
                config.getModelName(), config.getModelType(), config.getApiEndpoint());

        ModelParams params = parseModelParams(config.getModelParams());
        String modelName = params.model != null ? params.model : config.getModelName();
        String plainApiKey = decryptApiKey(config.getApiKey());

        OpenAiApi api = new OpenAiApi(config.getApiEndpoint(), plainApiKey);

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

        OpenAiChatModel chatModel = new OpenAiChatModel(api, optionsBuilder.build());
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 使用智谱AI SDK构建ChatClient
     * 智谱API不兼容OpenAI协议，需使用专属SDK
     */
    private ChatClient buildZhiPuChatClient(AiModelConfig config) {
        log.info("创建智谱AI ChatClient: model={}, type={}, endpoint={}",
                config.getModelName(), config.getModelType(), config.getApiEndpoint());

        ModelParams params = parseModelParams(config.getModelParams());
        String modelName = params.model != null ? params.model : config.getModelName();
        String plainApiKey = decryptApiKey(config.getApiKey());

        // 智谱API客户端：支持自定义baseUrl（私有化部署场景）
        ZhiPuAiApi api = StringUtils.hasText(config.getApiEndpoint())
                ? new ZhiPuAiApi(config.getApiEndpoint(), plainApiKey)
                : new ZhiPuAiApi(plainApiKey);

        // 构建智谱ChatOptions
        ZhiPuAiChatOptions.Builder optionsBuilder = ZhiPuAiChatOptions.builder()
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

        ZhiPuAiChatModel chatModel = new ZhiPuAiChatModel(api, optionsBuilder.build());
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 解密apiKey（兼容AES密文和明文）
     */
    private String decryptApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        try {
            return AesUtil.decrypt(apiKey);
        } catch (Exception e) {
            // 解密失败可能是历史明文数据，直接返回原始值
            log.warn("apiKey解密失败，可能为历史明文数据");
            return apiKey;
        }
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
