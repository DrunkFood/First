package com.jy.eleaitender.common.dto;

import lombok.Data;

/**
 * 文档修复替换项
 */
@Data
public class FixReplacement {

    /** 要查找的原始文本 */
    private String original;

    /** 替换文本 */
    private String targeted;

    /** 位置索引（可为null，兼容旧数据） */
    private LocationRefVO locationRef;
}
