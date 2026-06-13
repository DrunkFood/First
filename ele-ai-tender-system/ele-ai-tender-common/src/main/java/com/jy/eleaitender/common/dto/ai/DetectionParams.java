package com.jy.eleaitender.common.dto.ai;

import lombok.Data;

/**
 * 检测任务参数（敏感词/错别字/政策审查/格式规范共用）
 */
@Data
public class DetectionParams implements AiTaskParams {

    /** 内容文件ID（与content二选一） */
    private Long contentFileId;

    /** 待检测文本（与contentFileId二选一） */
    private String content;
}
