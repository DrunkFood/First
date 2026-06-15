package com.jy.eleaitender.ai.processor.generator;

import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.dto.ReviewConfig;
import com.jy.eleaitender.common.dto.ai.ReviewItemGenerateParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ReviewType;
import com.jy.eleaitender.common.exception.AiErrorContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

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
    private AiCallRecorder aiCallRecorder;

    /**
     * 执行评审项生成
     *
     * @param task AI任务（requestParams包含项目信息和需求内容）
     * @return JSON结果字符串 {"reviewItems": [...]}
     */
    public String generate(AiTask task) {
        log.info("开始评审项生成: taskId={}", task.getId());

        ReviewItemGenerateParams params = resultParser.parseParams(task.getRequestParams(), ReviewItemGenerateParams.class);

        String enabledTypes;
        if (StringUtils.hasText(params.getReviewConfig())) {
            // 有配置：生成所有启用的类型（generateStandard=false的类型由同步处理器替换二级节点为占位）
            ReviewConfig config = ReviewConfig.fromJson(params.getReviewConfig());
            enabledTypes = config.getEnabledTypes().stream()
                    .map(t -> ReviewType.fromCode(t.getReviewType()).getLabel())
                    .collect(Collectors.joining("、"));

            if (!StringUtils.hasText(enabledTypes)) {
                log.info("无启用的评审类型，跳过AI调用: taskId={}", task.getId());
                return "{\"reviewItems\":[]}";
            }
        } else {
            enabledTypes = ReviewType.getLabels();
        }

        // 构建Prompt
        String userPrompt = PromptBuilder.buildReviewItemGenerate(
                params.getProjectName(),
                params.getProjectType(),
                params.getProjectCategory(),
                params.getBudget(),
                params.getRequirementContent(),
                params.getReviewMethod(),
                enabledTypes
        );

        // 路由到合适的模型
        ChatClient client = modelRouter.route(AiTaskType.REVIEW_ITEM_GENERATE);

        // 同步调用并记录响应
        String aiOutput = aiCallRecorder.callAndRecord(client, SystemPromptTemplates.REVIEW_ITEM_GENERATE,
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

}
