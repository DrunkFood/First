package com.jy.eleaitender.ai.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.model.ModelRouter;
import com.jy.eleaitender.ai.prompt.PromptBuilder;
import com.jy.eleaitender.ai.prompt.PromptTemplates;
import com.jy.eleaitender.ai.recorder.AiCallRecorder;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 需求生成器
 * 通过AI模型根据项目信息生成结构化的业务需求内容
 */
@Slf4j
@Component
public class RequirementGenerator {

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private GenerateResultParser resultParser;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    /**
     * 执行需求生成
     *
     * @param task AI任务（requestParams包含项目信息）
     * @return JSON结果字符串 {"content": "Markdown需求内容"}
     */
    public String generate(AiTask task) {
        log.info("开始需求生成: taskId={}", task.getId());

        Map<String, Object> params = parseParams(task.getRequestParams());

        // 构建Prompt
        String userPrompt = PromptBuilder.buildRequirementGenerate(
                getString(params, "requirementName"),
                getString(params, "projectType"),
                getString(params, "projectCategory"),
                getString(params, "budget"),
                getString(params, "description"),
                getString(params, "referenceContent")
        );

        // 路由到合适的模型
        ChatClient client = modelRouter.route(AiTaskType.REQUIREMENT_GENERATE);

        // 同步调用并记录响应
        String aiOutput = aiCallRecorder.callAndRecord(client, PromptTemplates.REQUIREMENT_GENERATE,
                userPrompt, "GENERATION", task.getId(), task.getCreateId());

        // 提取Markdown内容
        String content = resultParser.extractMarkdown(aiOutput);

        log.info("需求生成完成: taskId={}, contentLength={}", task.getId(), content.length());
        return toJsonResult("content", content);
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
