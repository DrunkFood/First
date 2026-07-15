package com.jy.eleaitender.ai.processor.checker;

import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.common.enums.DetectionType;
import org.springframework.stereotype.Component;

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
        return DetectionType.FORMAT_CHECK.getCode();
    }

    @Override
    protected String getSystemPrompt() {
        return SystemPromptTemplates.DETECTION_FORMAT_CHECK;
    }

    @Override
    protected String buildUserPrompt(String content) {
        return PromptBuilder.buildDetection(content);
    }
}
