package com.jy.eleaitender.ai.checker;

import com.jy.eleaitender.ai.prompt.PromptBuilder;
import com.jy.eleaitender.ai.prompt.PromptTemplates;
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
        return "TYPO";
    }

    @Override
    protected String getSystemPrompt() {
        return PromptTemplates.DETECTION_TYPO;
    }

    @Override
    protected String buildUserPrompt(String content, Map<String, Object> params) {
        return PromptBuilder.buildDetection(content);
    }
}
