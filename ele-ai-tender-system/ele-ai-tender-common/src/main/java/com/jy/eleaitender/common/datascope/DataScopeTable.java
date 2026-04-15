package com.jy.eleaitender.common.datascope;

import java.util.Set;

/**
 * 数据隔离表配置
 * 声明哪些表需要按 create_id 进行数据隔离
 */
public final class DataScopeTable {

    /** 隔离字段名 */
    public static final String SCOPE_COLUMN = "create_id";

    /**
     * 需要按 create_id 隔离的表名集合
     * 不在此集合中的表不会被拦截器过滤
     */
    public static final Set<String> ISOLATED_TABLES = Set.of(
            "ai_project",
            "ai_requirement",
            "ai_review_item",
            "ai_detection_record",
            "ai_project_version",
            "ai_task",
            "file_info"
    );

    private DataScopeTable() {
    }
}
