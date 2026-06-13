package com.jy.eleaitender.ai.processor.checker;

import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.prompt.UserPromptTemplates;
import com.jy.eleaitender.common.enums.DetectionType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 错别字检测器
 * 检测错别字、语法错误、标点符号错误
 */
@Component
public class TypoDetector extends BaseDetector {

    @Override
    protected Boolean needFileFlag() {
        return false;
    }

    @Override
    protected String getDetectionType() {
        return DetectionType.TYPO.getCode();
    }

    @Override
    protected String getSystemPrompt() {
        return SystemPromptTemplates.DETECTION_TYPO;
    }

    @Override
    protected String buildUserPrompt(String content) {
        return PromptBuilder.buildDetection(content);
    }
}
