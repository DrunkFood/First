package com.jy.eleaitender.common.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 需求文档章节大纲
 * <p>
 * 定义单个章节的生成指引，由Step1大纲生成阶段产出，
 * 供Step2分章生成阶段使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequirementOutline {

    /**
     * 章节英文标识（如 project_overview、technical_specs）
     * 用于降级模板中的章节唯一标识
     */
    private String chapterKey;

    /**
     * 章节标题（如"项目概况与采购范围"）
     */
    private String chapterTitle;

    /**
     * 核心要点，描述该章节应涵盖的具体子主题和内容方向
     * 作为Step2章节生成的详细指引
     */
    private String corePoints;

    /**
     * 预估字数，指导AI控制该章节的输出长度
     */
    private int estimatedWords;
}
