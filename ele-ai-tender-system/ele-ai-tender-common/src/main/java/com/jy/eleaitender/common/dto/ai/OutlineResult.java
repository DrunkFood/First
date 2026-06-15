package com.jy.eleaitender.common.dto.ai;

import lombok.Data;
import java.util.List;

/**
 * 需求文档大纲结果
 * <p>
 * Step1大纲生成阶段AI输出的结构化结果，包含项目概况摘要和章节列表。
 * 用于指导Step2分章生成的并行执行。
 */
@Data
public class OutlineResult {

    /**
     * 项目概况摘要（50-100字）
     * 为Step2各章节生成提供统一的上下文信息
     */
    private String projectOverview;

    /**
     * 章节大纲列表
     * 每个章节定义标题、核心要点和预估字数
     */
    private List<RequirementOutline> chapters;
}
