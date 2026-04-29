package com.jy.eleaitender.common.dto;

import lombok.Data;

/**
 * 检测问题位置索引
 * 用于精确定位Word文档中的段落或表格单元格
 */
@Data
public class LocationRefVO {

    /** 位置类型: "paragraph" | "table" */
    private String type;

    /** IBodyElement序号（文档顶层元素序号，段落和表格共享） */
    private Integer elementIndex;

    /** 表格序号（table类型，在文档中的表格序号） */
    private Integer tableIndex;

    /** 行序号（table类型） */
    private Integer rowIndex;

    /** 列序号（table类型） */
    private Integer cellIndex;
}
