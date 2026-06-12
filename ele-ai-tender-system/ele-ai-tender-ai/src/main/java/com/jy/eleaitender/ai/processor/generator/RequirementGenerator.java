package com.jy.eleaitender.ai.processor.generator;

import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.prompt.UserPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.apache.commons.collections4.MapUtils.getString;

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
    private AiCallRecorder aiCallRecorder;

    /**
     * 执行需求生成
     *
     * @param task AI任务（requestParams包含项目信息）
     * @return JSON结果字符串 {"content": "Markdown需求内容"}
     */
    public String generate(AiTask task) {
        log.info("开始需求生成: taskId={}", task.getId());

        Map<String, Object> params = resultParser.parseParams(task.getRequestParams());

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
        String aiOutput = aiCallRecorder.callAndRecord(client, SystemPromptTemplates.REQUIREMENT_GENERATE,
                userPrompt, "GENERATION", task.getId(), task.getCreateId(), task.getFileIdList());

        // 提取Markdown内容
        String content = resultParser.extractMarkdown(aiOutput);

        log.info("需求生成完成: taskId={}, contentLength={}", task.getId(), content.length());
        return resultParser.toJsonResult("content", content);
    }

}
