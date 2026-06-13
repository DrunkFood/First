package com.jy.eleaitender.ai.processor.prompt;

/**
 * User Prompt模板常量
 */
public final class UserPromptTemplates {

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

    /**
     * 需求生成 - User Prompt 模板
     * 参数: projectName, projectType, budget, description
     */
    public static final String REQUIREMENT_GENERATE_USER = """
            请根据以下项目信息生成业务需求：
            
            项目名称：%s
            项目类型：%s
            项目预算：%s元
            项目描述：%s
            """;

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
            请确保总分值为100分，合理分配各评审项的分值。
            """;

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
}
