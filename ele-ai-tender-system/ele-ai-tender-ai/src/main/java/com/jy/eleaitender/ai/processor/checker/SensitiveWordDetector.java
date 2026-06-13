package com.jy.eleaitender.ai.processor.checker;

import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.common.enums.DetectionType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 敏感词检测器
 * 检测歧视性、限制性、排他性、倾向性表述
 */
@Component
public class SensitiveWordDetector extends BaseDetector {

    @Override
    protected Boolean needFileFlag() {
        return false;
    }

    @Override
    protected String getDetectionType() {
        return DetectionType.SENSITIVE_WORD.getCode();
    }

    @Override
    protected String getSystemPrompt() {
        return SystemPromptTemplates.DETECTION_SENSITIVE_WORD;
    }

    @Override
    protected String buildUserPrompt(String content) {
        return PromptBuilder.buildDetection(content);
    }
}
