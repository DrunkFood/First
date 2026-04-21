package com.jy.eleaitender.ai.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.model.ModelRouter;
import com.jy.eleaitender.ai.prompt.PromptBuilder;
import com.jy.eleaitender.ai.prompt.PromptTemplates;
import com.jy.eleaitender.ai.recorder.AiCallRecorder;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.exception.AiErrorContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 评审项生成器
 * 通过AI模型根据项目信息和需求内容生成评审标准体系
 */
@Slf4j
@Component
public class ReviewItemGenerator {

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private GenerateResultParser resultParser;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    /**
     * 执行评审项生成
     *
     * @param task AI任务（requestParams包含项目信息和需求内容）
     * @return JSON结果字符串 {"reviewItems": [...]}
     */
    public String generate(AiTask task) {
        log.info("开始评审项生成: taskId={}", task.getId());

        Map<String, Object> params = parseParams(task.getRequestParams());

        // 构建Prompt
        String userPrompt = PromptBuilder.buildReviewItemGenerate(
                getString(params, "projectName"),
                getString(params, "projectType"),
                getString(params, "projectCategory"),
                getString(params, "budget"),
                getString(params, "requirementContent"),
                getString(params, "reviewMethod")
        );

        // 路由到合适的模型
        ChatClient client = modelRouter.route(AiTaskType.REVIEW_ITEM_GENERATE);

        // 同步调用并记录响应
        String aiOutput = aiCallRecorder.callAndRecord(client, PromptTemplates.REVIEW_ITEM_GENERATE,
                userPrompt, "GENERATION", task.getId(), task.getCreateId(), task.getFileIdList());

        // 提取JSON内容
        String jsonResult = resultParser.extractJson(aiOutput);

        // 验证结果包含reviewItems字段
        if (!resultParser.validateJsonField(jsonResult, "reviewItems")) {
            log.warn("评审项生成结果缺少reviewItems字段, taskId={}", task.getId());
            throw new AiErrorContentException("AI输出格式异常，请重试", jsonResult);
        }

        log.info("评审项生成完成: taskId={}", task.getId());
        return jsonResult;
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
}
