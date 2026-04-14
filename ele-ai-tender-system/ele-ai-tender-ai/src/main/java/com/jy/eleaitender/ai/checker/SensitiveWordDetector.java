package com.jy.eleaitender.ai.checker;

import com.jy.eleaitender.ai.prompt.PromptBuilder;
import com.jy.eleaitender.ai.prompt.PromptTemplates;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 敏感词检测器
 * 检测歧视性、限制性、排他性、倾向性表述
 */
@Component
public class SensitiveWordDetector extends BaseDetector {

    @Override
    protected String getDetectionType() {
        return "SENSITIVE_WORD";
    }

    @Override
    protected String getSystemPrompt() {
        return PromptTemplates.DETECTION_SENSITIVE_WORD;
    }

    @Override
    protected String buildUserPrompt(String content, Map<String, Object> params) {
        return PromptBuilder.buildDetection(content);
    }
}
