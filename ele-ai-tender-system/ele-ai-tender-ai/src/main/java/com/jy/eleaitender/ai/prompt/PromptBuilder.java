package com.jy.eleaitender.ai.prompt;

/**
 * Prompt动态构建器
 * 根据不同场景组装User Prompt
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    /**
     * 构建需求生成的User Prompt
     */
    public static String buildRequirementGenerate(String projectName, String projectType,
                                                   String projectCategory, String budget,
                                                   String description, String referenceContent) {
        return String.format(PromptTemplates.REQUIREMENT_GENERATE_USER,
                defaultStr(projectName),
                defaultStr(projectType),
                defaultStr(projectCategory),
                defaultStr(budget),
                defaultStr(description),
                referenceContent != null ? referenceContent : "无参考文档");
    }

    /**
     * 构建评审项生成的User Prompt
     */
    public static String buildReviewItemGenerate(String projectName, String projectType,
                                                  String projectCategory, String budget,
                                                  String requirementContent, String reviewMethod) {
        return String.format(PromptTemplates.REVIEW_ITEM_GENERATE_USER,
                defaultStr(projectName),
                defaultStr(projectType),
                defaultStr(projectCategory),
                defaultStr(budget),
                defaultStr(requirementContent),
                defaultStr(reviewMethod, "综合评分法"));
    }

    /**
     * 构建文本优化的User Prompt
     */
    public static String buildTextOptimize(String content, String requirement) {
        return String.format(PromptTemplates.TEXT_OPTIMIZE_USER,
                defaultStr(content),
                defaultStr(requirement, "提升专业性和规范性"));
    }

    /**
     * 构建检测类的User Prompt（敏感词/错别字/格式检测通用）
     */
    public static String buildDetection(String content) {
        return String.format(PromptTemplates.DETECTION_USER, defaultStr(content));
    }

    /**
     * 构建政策审查的User Prompt
     */
    public static String buildPolicyReview(String content, String policyContent) {
        return String.format(PromptTemplates.DETECTION_POLICY_USER,
                defaultStr(content),
                defaultStr(policyContent, "暂无政策文件内容"));
    }

    private static String defaultStr(String value) {
        return value != null ? value : "";
    }

    private static String defaultStr(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
