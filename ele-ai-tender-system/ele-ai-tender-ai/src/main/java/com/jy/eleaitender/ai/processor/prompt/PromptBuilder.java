package com.jy.eleaitender.ai.processor.prompt;

import com.jy.eleaitender.common.enums.ProjectCategory;
import com.jy.eleaitender.common.enums.ProjectType;
import com.jy.eleaitender.common.enums.ReviewMethod;
import com.jy.eleaitender.common.enums.ReviewType;

/**
 * Prompt动态构建器
 * 根据不同场景组装User Prompt
 * <p>
 * 注意：Spring AI 的 ChatClient.prompt().system() / .user() 会通过 PromptTemplate 处理字符串，
 * PromptTemplate 使用 {variableName} 语法做变量占位。如果模板内容包含字面量花括号（如 JSON 示例），
 * 必须使用双花括号转义：{ → {{，} → }}，否则 PromptTemplate 构造时会抛出
 * IllegalArgumentException: The template string is not valid.
 */
public final class PromptBuilder {

    // ===========================文本优化===========================

    /**
     * 构建文本优化的User Prompt
     */
    public static String buildTextOptimize(String content, String requirement) {
        return String.format(UserPromptTemplates.TEXT_OPTIMIZE_USER,
                defaultStr(content),
                defaultStr(requirement, "提升专业性和规范性"));
    }

    // ===========================需求生成===========================

    /**
     * 构建需求生成的User Prompt（已废弃，由三步式Agent替代：buildOutline / buildChapter / buildReview）
     */
    @Deprecated
    public static String buildRequirementGenerate(String projectName,
                                                  String projectType,
                                                  String projectCategory,
                                                  String budget,
                                                  String description) {
        return String.format(UserPromptTemplates.REQUIREMENT_GENERATE_USER,
                defaultStr(projectName),
                defaultStr(ProjectType.fromCode(projectType).getLabel()),
                defaultStr(ProjectCategory.fromCode(projectCategory).getLabel()),
                defaultStr(budget),
                defaultStr(description));
    }

    /**
     * 构建需求大纲生成的User Prompt
     */
    public static String buildOutline(String projectName, String projectType,
                                      String projectCategory, String budget,
                                      String description) {
        return String.format(UserPromptTemplates.REQUIREMENT_OUTLINE_GENERATE_USER,
                defaultStr(projectName),
                defaultStr(ProjectType.fromCode(projectType).getLabel()),
                defaultStr(ProjectCategory.fromCode(projectCategory).getLabel()),
                defaultStr(budget),
                defaultStr(description));
    }

    /**
     * 构建需求章节生成的User Prompt
     */
    public static String buildChapter(String projectOverview, String outlineDirectory,
                                      String chapterTitle, String corePoints,
                                      int estimatedWords) {
        return String.format(UserPromptTemplates.REQUIREMENT_CHAPTER_GENERATE_USER,
                defaultStr(projectOverview),
                defaultStr(outlineDirectory),
                defaultStr(chapterTitle),
                defaultStr(corePoints),
                defaultStr(String.valueOf(estimatedWords), "3000"));
    }

    /**
     * 构建需求审查的User Prompt
     */
    public static String buildReview(String projectName, String projectType,
                                     String projectCategory, String budget,
                                     String fullContent) {
        return String.format(UserPromptTemplates.REQUIREMENT_REVIEW_USER,
                defaultStr(projectName),
                defaultStr(ProjectType.fromCode(projectType).getLabel()),
                defaultStr(ProjectCategory.fromCode(projectCategory).getLabel()),
                defaultStr(budget),
                defaultStr(fullContent));
    }

    // ===========================评审项生成===========================

    /**
     * 构建评审项生成的 User Prompt（带评审类型配置）
     */
    public static String buildReviewItemGenerate(String projectName, String projectType,
                                                 String projectCategory, String budget,
                                                 String requirementContent, String reviewMethod,
                                                 String enabledTypes) {
        return String.format(UserPromptTemplates.REVIEW_ITEM_GENERATE_USER_WITH_CONFIG,
                defaultStr(projectName),
                defaultStr(ProjectType.fromCode(projectType).getLabel()),
                defaultStr(ProjectCategory.fromCode(projectCategory).getLabel()),
                defaultStr(budget),
                defaultStr(requirementContent),
                defaultStr(ReviewMethod.fromCode(reviewMethod).getLabel()),
                defaultStr(enabledTypes, ReviewType.getLabels()));
    }

    // ===========================检测类===========================

    /**
     * 构建检测类的User Prompt（敏感词/错别字/格式检测通用）
     */
    public static String buildDetection(String content) {
        return String.format(UserPromptTemplates.DETECTION_USER,
                defaultStr(content));
    }

    /**
     * 构建政策审查的User Prompt
     */
    public static String buildPolicyReview(String content) {
        return String.format(UserPromptTemplates.DETECTION_POLICY_USER,
                defaultStr(content));
    }

    // ===========================占位符匹配===========================

    /**
     * 构建占位符匹配的User Prompt
     */
    public static String buildPlaceholderMatch(String placeholders, String dataFields) {
        return String.format(UserPromptTemplates.PLACEHOLDER_MATCH_USER,
                defaultStr(placeholders),
                defaultStr(dataFields));
    }

    private static String defaultStr(String value) {
        return value != null ? value : "";
    }

    private static String defaultStr(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
