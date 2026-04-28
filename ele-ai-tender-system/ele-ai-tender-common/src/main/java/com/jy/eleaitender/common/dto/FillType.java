package com.jy.eleaitender.common.dto;

/**
 * 文档填充数据类型
 */
public enum FillType {

    /** 文本，poi-tl {{key}} 替换 */
    TEXT,

    /** 表格，POI 编程生成，模板中 {{key}} 标记位置 */
    TABLE,

    /** 图片，poi-tl {{@key}} 替换 */
    IMAGE,

    /** Markdown文本，flexmark→HTML→poi-tl HtmlRenderPolicy 渲染 */
    MARKDOWN
}
