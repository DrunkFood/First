package com.jy.eleaitender.ai.model;

import com.jy.eleaitender.common.entity.support.AiModelConfig;
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
 * 模型路由器
 * 根据任务类型/使用场景，从数据库路由规则中选择合适的模型创建ChatClient
 */
@Slf4j
@Component
public class ModelRouter {

    @Autowired
    private ModelConfigCacheService cacheService;

    @Autowired
    private DynamicChatClientFactory clientFactory;

    /**
     * 根据任务类型路由到合适的ChatClient
     */
    public ChatClient route(AiTaskType taskType) {
        AiUsageScenario scenario = AiUsageScenario.resolveScenario(taskType);
        return route(scenario);
    }

    /**
     * 根据使用场景路由到合适的ChatClient
     */
    public ChatClient route(AiUsageScenario scenario) {
        List<SupModelRouteRule> rules = cacheService.getActiveRouteRules(scenario.getCode());

        if (rules.isEmpty()) {
            throw new AiUnavailableException("场景[" + scenario.getLabel() + "]无可用路由规则");
        }

        // 按优先级遍历规则
        for (SupModelRouteRule rule : rules) {
            // 尝试优先模型
            ChatClient client = tryCreateClient(rule.getPrimaryModelId(), "优先");
            if (client != null) {
                return client;
            }

            // 优先模型不可用，尝试降级模型
            if (rule.getFallbackModelId() != null) {
                client = tryCreateClient(rule.getFallbackModelId(), "降级");
                if (client != null) {
                    return client;
                }
            }
        }

        throw new AiUnavailableException("场景[" + scenario.getLabel() + "]所有模型均不可用");
    }

    /**
     * 尝试根据模型ID创建ChatClient
     */
    private ChatClient tryCreateClient(Long modelId, String roleLabel) {
        AiModelConfig config = cacheService.getModelConfig(modelId);
        if (config == null) {
            log.warn("{}模型不存在: modelId={}", roleLabel, modelId);
            return null;
        }
        if (config.getIsActive() == null || config.getIsActive() != 1) {
            log.warn("{}模型已停用: modelId={}, name={}", roleLabel, modelId, config.getModelName());
            return null;
        }
        try {
            return clientFactory.getOrCreateChatClient(config);
        } catch (Exception e) {
            log.error("创建{}模型ChatClient失败: modelId={}, name={}", roleLabel, modelId, config.getModelName(), e);
            return null;
        }
    }

}
