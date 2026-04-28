package com.jy.eleaitender.common.dto;

import lombok.Data;

/**
 * 表格列定义
 */
@Data
public class ColumnDef {

    /** 列标识（英文），用于行数据Map的key */
    private String key;

    /** 表头显示文本 */
    private String header;

    /** 列宽（EMU，0=自动） */
    private int width;

    public ColumnDef() {}

    public ColumnDef(String key, String header) {
        this.key = key;
        this.header = header;
    }

    public ColumnDef(String key, String header, int width) {
        this.key = key;
        this.header = header;
        this.width = width;
    }
}
