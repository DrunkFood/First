package com.jy.eleaitender.common.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 表格填充数据，含列定义、行数据和合并规则
 */
@Data
public class TableData {

    /** 列定义 */
    private List<ColumnDef> columns;

    /** 行数据，key 为 ColumnDef.key */
    private List<Map<String, String>> rows;

    /** 合并规则 */
    private List<MergeRule> mergeRules;
}
