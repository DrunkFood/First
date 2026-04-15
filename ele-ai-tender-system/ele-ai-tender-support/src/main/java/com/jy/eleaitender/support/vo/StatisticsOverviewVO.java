package com.jy.eleaitender.support.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 统计概览 VO
 */
@Data
@Schema(description = "统计分析概览数据")
public class StatisticsOverviewVO {

    @Schema(description = "用户总数")
    private long userCount;

    @Schema(description = "角色总数")
    private long roleCount;

    @Schema(description = "接入系统数")
    private long accessSystemCount;

    @Schema(description = "版本总数")
    private long versionCount;

    @Schema(description = "项目总数")
    private long projectCount;

    @Schema(description = "需求总数")
    private long requirementCount;

    @Schema(description = "模板总数")
    private long templateCount;

    @Schema(description = "知识文档总数")
    private long knowledgeCount;

    @Schema(description = "模型配置总数")
    private long modelConfigCount;

    @Schema(description = "操作日志总数")
    private long operationLogCount;

    @Schema(description = "政策文件总数")
    private long policyFileCount;

    @Schema(description = "今日操作次数")
    private long todayOperationCount;

    @Schema(description = "今日新建项目数")
    private long todayProjectCount;

    @Schema(description = "未读消息总数")
    private long unreadMessageCount;

    @Schema(description = "近7天每日操作统计")
    private List<DailyStatItem> dailyOperations;

    /**
     * 每日统计项
     */
    @Data
    @Schema(description = "每日统计项")
    public static class DailyStatItem {

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "数量")
        private long count;
    }
}
