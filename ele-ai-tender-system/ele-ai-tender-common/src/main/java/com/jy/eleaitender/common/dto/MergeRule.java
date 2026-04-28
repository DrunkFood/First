package com.jy.eleaitender.common.dto;

import lombok.Data;

/**
 * 表格单元格合并规则
 */
@Data
public class MergeRule {

    /** 要合并的列索引（0-based） */
    private int columnIndex;

    /** 合并策略 */
    private MergeStrategy strategy;

    public MergeRule() {}

    public MergeRule(int columnIndex, MergeStrategy strategy) {
        this.columnIndex = columnIndex;
        this.strategy = strategy;
    }
}
