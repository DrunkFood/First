package com.jy.eleaitender.ai.processor.checker;

import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.prompt.UserPromptTemplates;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 政策审查检测器
 * 对照政策文件检查招标文件的合规性
 */
@Component
public class PolicyReviewDetector extends BaseDetector {

    @Override
    protected Boolean needFileFlag() {
        return true;
    }

    @Override
    protected String getDetectionType() {
        return "POLICY_REVIEW";
    }

    @Override
    protected String getSystemPrompt() {
        return SystemPromptTemplates.DETECTION_POLICY_REVIEW;
    }

    @Override
    protected String buildUserPrompt(String content, Map<String, Object> params) {
        return PromptBuilder.buildPolicyReview(content);
    }
}
