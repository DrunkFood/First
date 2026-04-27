package com.jy.eleaitender.common.dto.response;

import lombok.Data;

/**
 * Word文档修复结果
 */
@Data
public class WordFixResultVO {

    /** 修复后的文件ID */
    private Long fileId;

    /** 成功替换次数 */
    private int fixedCount;

    /** 未找到原文次数 */
    private int failedCount;
}
