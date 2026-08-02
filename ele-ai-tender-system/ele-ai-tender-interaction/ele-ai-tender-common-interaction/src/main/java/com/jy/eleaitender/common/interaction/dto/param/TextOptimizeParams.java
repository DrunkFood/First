package com.jy.eleaitender.common.interaction.dto.param;

import lombok.Data;

/**
 * 文本优化任务参数
 */
@Data
public class TextOptimizeParams implements AiTaskParams {

    private String content;

    /**
     * 优化要求（可选，默认"提升专业性和规范性"）
     */
    private String requirement;

}
