package com.jy.eleaitender.ai.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.model.ModelRouter;
import com.jy.eleaitender.ai.prompt.PromptBuilder;
import com.jy.eleaitender.ai.prompt.PromptTemplates;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文本优化器
 * 通过AI模型优化招标文件文本内容
 */
@Slf4j
@Component
public class TextOptimizer {

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 执行文本优化（任务队列模式，用于批量文本优化）
     *
     * @param task AI任务（requestParams包含待优化文本和要求）
     * @return JSON结果字符串 {"optimizedContent": "优化后的文本"}
     */
    public String optimize(AiTask task) {
        log.info("开始文本优化: taskId={}", task.getId());

        Map<String, Object> params = parseParams(task.getRequestParams());

        String content = getString(params, "content");
        String requirement = getString(params, "requirement");

        // 构建Prompt
        String userPrompt = PromptBuilder.buildTextOptimize(content, requirement);

        // 路由到合适的模型
        ChatClient client = modelRouter.route(AiTaskType.TEXT_OPTIMIZE);

        // 同步调用
        String optimized = client.prompt()
                .system(PromptTemplates.TEXT_OPTIMIZE)
                .user(userPrompt)
                .call()
                .content();

        log.info("文本优化完成: taskId={}", task.getId());
        return toJsonResult("optimizedContent", optimized != null ? optimized : "");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParams(String requestParams) {
        try {
            return objectMapper.readValue(requestParams, Map.class);
        } catch (Exception e) {
            log.warn("解析requestParams失败: {}", requestParams, e);
            return Map.of();
        }
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    private String toJsonResult(String key, String value) {
        try {
            return objectMapper.writeValueAsString(Map.of(key, value));
        } catch (Exception e) {
            return "{\"" + key + "\": \"\"}";
        }
    }
}
