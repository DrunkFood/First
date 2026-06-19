package com.jy.eleaitender.ai.processor.prompt;

/**
 * User Prompt模板常量
 */
public final class UserPromptTemplates {

    // ===========================文本优化===========================

    /**
     * 文本优化 - User Prompt 模板
     * 参数: content, requirement
     */
    public static final String TEXT_OPTIMIZE_USER = """
            请优化以下招标文件文本：
            
            原文：
            %s
            
            优化要求：%s
            """;

    // ===========================需求生成===========================

    /**
     * 需求生成 - User Prompt 模板（已废弃，由三步式Agent替代）
     */
    @Deprecated
    public static final String REQUIREMENT_GENERATE_USER = """
            请根据以下项目信息生成业务需求：
            
            项目名称：%s
            项目类型：%s
            项目类别：%s
            项目预算：%s元
            项目描述：%s
            """;

    /**
     * 需求大纲生成 - User Prompt 模板
     * 参数: projectName, projectType, projectCategory, budget, description
     */
    public static final String REQUIREMENT_OUTLINE_GENERATE_USER = """
            请根据以下项目信息规划招标需求文档的大纲：
            
            项目名称：%s
            项目类型：%s
            项目类别：%s
            项目预算：%s元
            项目描述：%s
            """;

    /**
     * 需求章节生成 - User Prompt 模板
     * 参数: projectOverview, outlineDirectory, chapterTitle, corePoints, estimatedWords
     */
    public static final String REQUIREMENT_CHAPTER_GENERATE_USER = """
            项目概况：%s
            
            完整大纲目录：
            %s
            
            当前需要编写的章节：
            章节标题：%s
            核心要点：%s
            预估字数上限：%s字
            
            请在该字数上限内编写本章节的详细内容：
            """;

    /**
     * 需求审查 - User Prompt 模板
     * 参数: projectName, projectType, projectCategory, budget, fullContent
     */
    public static final String REQUIREMENT_REVIEW_USER = """
            项目信息：%s，%s，%s，预算%s元
            
            请审查以下招标需求文档：
            
            %s
            """;

    // ===========================评审项生成===========================

    /**
     * 评审项生成 - User Prompt 模板（带评审类型配置）
     * 参数: projectName, projectType, projectCategory, budget, requirementContent, reviewMethod, enabledTypes
     */
    public static final String REVIEW_ITEM_GENERATE_USER_WITH_CONFIG = """
            请根据以下项目信息和需求内容生成评审标准：
            
            项目名称：%s
            项目类型：%s
            项目类别：%s
            项目预算：%s元
            
            业务需求内容：
            %s
            
            评审方式：%s
            
            评审类型配置（只生成以下列出的类型，严禁添加其他类型）：
            %s
            
            输出JSON的一级分类name必须严格使用上述类型名称。

            硬性计分规则：
            1. 符合性审查不计入100分，符合性审查节点score必须为0或省略。
            2. 只给资信评审、技术评审、商务评审中的叶子评审项设置score；分类节点和有children的非叶子节点score必须为0或省略。
            3. 在上述评审类型配置范围内，所有非符合性评审类型的叶子节点score合计必须正好等于100分。
            4. 输出前必须逐项累加自检；不要输出总分不等于100分的JSON。
            """;

    // ===========================检测类===========================

    /**
     * 检测类 - User Prompt 模板（通用）
     * 参数: content
     */
    public static final String DETECTION_USER = """
            请检测以下招标文件内容：
            
            %s
            """;

    /**
     * 政策审查 - User Prompt 模板
     * 参数: content, policyContent
     */
    public static final String DETECTION_POLICY_USER = """
            请对照政策文件检查以下招标文件内容的合规性，政策文件位于【文档内容】：
            
            招标文件内容：
            %s
            """;

    // ===========================占位符匹配===========================

    /**
     * 占位符匹配 - User Prompt 模板
     * 参数: placeholderList, dataFieldList
     */
    public static final String PLACEHOLDER_MATCH_USER = """
            请将以下Word模板占位符与数据字段进行匹配：
            
            模板占位符：
            %s
            
            可用数据字段：
            %s
            """;
}
