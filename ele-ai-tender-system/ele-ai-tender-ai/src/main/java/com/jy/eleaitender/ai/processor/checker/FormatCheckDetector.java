package com.jy.eleaitender.ai.processor.checker;

import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.PromptTemplates;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 格式规范检测器
 * 检查标题层级、编号格式、必要章节完整性
 */
@Component
public class FormatCheckDetector extends BaseDetector {

    @Override
    protected Boolean needFileFlag() {
        return false;
    }

    @Override
    protected String getDetectionType() {
        return "FORMAT_CHECK";
    }

    @Override
    protected String getSystemPrompt() {
        return PromptTemplates.DETECTION_FORMAT_CHECK;
    }

    @Override
    protected String buildUserPrompt(String content, Map<String, Object> params) {
        return PromptBuilder.buildDetection(content);
    }
}
