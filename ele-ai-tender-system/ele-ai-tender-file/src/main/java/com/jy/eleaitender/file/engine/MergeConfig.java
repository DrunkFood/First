package com.jy.eleaitender.file.engine;

import lombok.Data;

@Data
public class MergeConfig {
    /** 要合并的表格索引（0-based） */
    private int tableIndex;
    /** 要合并的列索引（0-based） */
    private int columnIndex;
}
