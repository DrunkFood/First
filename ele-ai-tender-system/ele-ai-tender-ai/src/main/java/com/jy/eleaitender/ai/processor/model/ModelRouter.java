package com.jy.eleaitender.ai.processor.model;

import com.jy.eleaitender.common.entity.support.SupModelConfig;
import com.jy.eleaitender.common.entity.support.SupModelRouteRule;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.AiUsageScenario;
import com.jy.eleaitender.common.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型路由器。
 * 根据任务类型/使用场景，从数据库路由规则中选择合适的模型创建 ChatClient。
 */
@Slf4j
@Component
public class ModelRouter {

    @Autowired
    private ModelConfigCacheService cacheService;

    @Autowired
    private DynamicChatClientFactory clientFactory;

    /**
     * 根据任务类型路由到合适的 ChatClient。
     */
    public ChatClient route(AiTaskType taskType) {
        return routeWithInfo(taskType).chatClient();
    }

    public RoutedChatClient routeWithInfo(AiTaskType taskType) {
        AiUsageScenario scenario = AiUsageScenario.resolveScenario(taskType);
        return routeWithInfo(scenario);
    }

    /**
     * 根据使用场景路由到合适的 ChatClient。
     */
    public ChatClient route(AiUsageScenario scenario) {
        return routeWithInfo(scenario).chatClient();
    }

    public RoutedChatClient routeWithInfo(AiUsageScenario scenario) {
        List<SupModelRouteRule> rules = cacheService.getActiveRouteRules(scenario.getCode());

        if (rules.isEmpty()) {
            throw new AiUnavailableException("场景[" + scenario.getLabel() + "]无可用路由规则");
        }

        for (SupModelRouteRule rule : rules) {
            RoutedChatClient routedClient = tryCreateClient(rule.getPrimaryModelId(), "优先");
            if (routedClient != null) {
                return routedClient;
            }

            if (rule.getFallbackModelId() != null) {
                routedClient = tryCreateClient(rule.getFallbackModelId(), "降级");
                if (routedClient != null) {
                    return routedClient;
                }
            }
        }

        throw new AiUnavailableException("场景[" + scenario.getLabel() + "]所有模型均不可用");
    }

    /**
     * 根据指定模型配置 ID 创建 ChatClient。
     */
    public ChatClient routeModel(Long modelId) {
        return routeModelWithInfo(modelId).chatClient();
    }

    public RoutedChatClient routeModelWithInfo(Long modelId) {
        RoutedChatClient routedClient = tryCreateClient(modelId, "指定");
        if (routedClient == null) {
            throw new AiUnavailableException("模型不可用: modelId=" + modelId);
        }
        return routedClient;
    }

    private RoutedChatClient tryCreateClient(Long modelId, String roleLabel) {
        SupModelConfig config = cacheService.getModelConfig(modelId);
        if (config == null) {
            log.warn("{}模型不存在: modelId={}", roleLabel, modelId);
            return null;
        }
        if (config.getIsActive() == null || config.getIsActive() != 1) {
            log.warn("{}模型已停用: modelId={}, name={}", roleLabel, modelId, config.getModelName());
            return null;
        }
        try {
            ChatClient client = clientFactory.getOrCreateChatClient(config);
            return new RoutedChatClient(client, clientFactory.resolveModelName(config));
        } catch (Exception e) {
            log.error("创建{}模型ChatClient失败: modelId={}, name={}", roleLabel, modelId, config.getModelName(), e);
            return null;
        }
    }
}
