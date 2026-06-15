package com.jy.eleaitender.ai.processor.generator;

import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.dto.ai.TextOptimizeParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private GenerateResultParser resultParser;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    /**
     * 执行文本优化（任务队列模式，用于批量文本优化）
     *
     * @param task AI任务（requestParams包含待优化文本和要求）
     * @return JSON结果字符串 {"optimizedContent": "优化后的文本"}
     */
    public String optimize(AiTask task) {
        log.info("开始文本优化: taskId={}", task.getId());

        TextOptimizeParams params = resultParser.parseParams(task.getRequestParams(), TextOptimizeParams.class);

        String content = params.getContent();
        String requirement = params.getRequirement();

        // 构建Prompt
        String userPrompt = PromptBuilder.buildTextOptimize(content, requirement);

        // 路由到合适的模型
        ChatClient client = modelRouter.route(AiTaskType.TEXT_OPTIMIZE);

        // 同步调用并记录响应
        String optimized = aiCallRecorder.callAndRecord(client, SystemPromptTemplates.TEXT_OPTIMIZE,
                userPrompt, "OPTIMIZATION", task.getId(), task.getCreateId(), task.getFileIdList());

        log.info("文本优化完成: taskId={}", task.getId());
        return resultParser.toJsonResult("optimizedContent", optimized != null ? optimized : "");
    }

}
