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
            项目预算金额：%s元
            项目原始描述：%s
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
            硬性字数上限：%s字
            该上限是强制约束，不是建议；正文总字数不得超过该数值，超出上限视为无效输出。
            
            请严格在上述硬性上限内编写本章节的详细内容：
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
            请根据以下项目信息和需求内容生成《综合评分法评审标准表》：
            
            项目名称：%s
            项目类型：%s
            项目类别：%s
            项目预算：%s元
            
            招标（采购）需求书（含项目类型、预算、技术/服务要求）：
            %s
            
            评审方式：%s
            
            如后续附带模板或参考文件内容，需结合模板要求生成评审项；模板要求与下方JSON结构或硬性计分规则冲突时，以JSON结构和硬性计分规则为准。
            
            评审类型配置（只生成以下列出的类型，严禁添加其他类型）：
            %s
            
            输出JSON的一级分类name必须严格使用上述类型名称。

            硬性计分规则：
            1. 符合性审查不计入100分，符合性审查节点score必须为0或省略。
            2. 只给资信评审、技术评审、商务评审中的叶子评审项设置score；分类节点和有children的非叶子节点score必须为0或省略。
            3. 在上述评审类型配置范围内，所有非符合性评审类型的叶子节点score合计必须正好等于100分。
            4. 输出前必须逐项累加自检；不要输出总分不等于100分的JSON。
            
            输出格式硬性要求：
            1. 根节点必须是reviewItems，且reviewItems必须是数组。
            2. 不要输出Markdown代码块，不要输出解释说明，不要输出JSON以外的任何文字。
            3. 不要使用data、result、content、output等外层包装字段。
            """;

    /**
     * 评审项生成 User Prompt（权重模式）
     * 参数: projectName, projectType, projectCategory, budget, requirementContent, reviewMethod, enabledTypes
     */
    public static final String REVIEW_ITEM_GENERATE_USER_WITH_CONFIG_WEIGHT = """
            请根据以下项目信息和需求内容生成《综合评分法评审标准表》（权重模式）：

            项目名称：%s
            项目类型：%s
            项目类别：%s
            项目预算：%s元

            招标（采购）需求书：
            %s

            评审方式：%s

            评审类型配置（只生成以下列出的类型，严禁添加其他类型）：
            %s

            输出JSON的一级分类name必须严格使用上述类型名称。

            硬性计分规则（权重模式）：
            1. 符合性审查不计分、不参与权重，其节点 score/weight 必须为0或省略。
            2. 技术标/资信标/商务评审的一级分类节点设置 weight（权重百分比），其下叶子节点设置 score；分类节点和有children的非叶子节点 score 必须为0或省略。
            3. 所有非符合性评分类型的 weight 合计必须正好等于100%%。
            4. 每个评分类型内，叶子节点 score 合计必须正好等于100分（该类型满分100）。
            5. 输出前必须逐项累加自检：权重合计=100%% 且 每类型内 score 合计=100分。

            输出格式硬性要求：
            1. 根节点必须是reviewItems，且reviewItems必须是数组。
            2. 不要输出Markdown代码块、解释说明或JSON以外文字。
            3. 不要使用data、result、content、output等外层包装字段。
            """;

    /**
     * 评审项JSON修复 - User Prompt 模板
     * 参数: aiOutput
     */
    public static final String REVIEW_ITEM_JSON_REPAIR_USER = """
            请只修复JSON格式，不要重新生成评审项内容。
            
            原始AI输出：
            %s
            
            目标JSON结构：
            {
              "reviewItems": [
                {
                  "name": "一级分类名称",
                  "level": 1,
                  "children": []
                }
              ]
            }
            
            要求：
            1. 只输出目标JSON，不要输出解释说明。
            2. 根节点必须是reviewItems，且reviewItems必须是数组。
            3. 尽量保留原始AI输出中的分类、评审项、评分标准、score、subjectivity、isRequired和children。
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
