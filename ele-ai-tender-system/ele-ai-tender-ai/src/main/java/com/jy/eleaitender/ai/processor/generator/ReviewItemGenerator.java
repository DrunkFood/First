package com.jy.eleaitender.ai.processor.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.model.RoutedChatClient;
import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.dto.ReviewConfig;
import com.jy.eleaitender.common.dto.ReviewTypeConfig;
import com.jy.eleaitender.common.dto.TemplateReviewItemConfig;
import com.jy.eleaitender.common.dto.ai.ReviewItemGenerateParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ReviewType;
import com.jy.eleaitender.common.exception.AiErrorContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评审项生成器
 * 通过AI模型根据项目信息和需求内容生成评审标准体系
 */
@Slf4j
@Component
public class ReviewItemGenerator {

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private GenerateResultParser resultParser;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Set<String> REVIEW_ITEM_ARRAY_FIELD_NAMES = Set.of(
            "reviewItems", "items", "reviewItemList", "评审项", "评审项列表", "评审标准", "评审标准表"
    );

    private static final Set<String> REVIEW_ITEM_WRAPPER_FIELD_NAMES = Set.of(
            "data", "result", "content", "output"
    );

    private static final Set<String> REVIEW_CATEGORY_NAMES = Set.of(
            "符合性审查", "符合性评审", "合规审查", "资格审查",
            "技术标评审", "技术评审",
            "资信标评审", "资信评审",
            "商务评审", "商务标评审", "价格评审"
    );

    /**
     * 执行评审项生成
     *
     * @param task AI任务（requestParams包含项目信息和需求内容）
     * @return JSON结果字符串 {"reviewItems": [...]}
     */
    public String generate(AiTask task) {
        log.info("开始评审项生成: taskId={}", task.getId());

        ReviewItemGenerateParams params = resultParser.parseParams(task.getRequestParams(), ReviewItemGenerateParams.class);

        String enabledTypes;
        BigDecimal aiScoreTotal = BigDecimal.valueOf(100);
        ReviewConfig config = null;
        if (StringUtils.hasText(params.getReviewConfig())) {
            // SCORE模式只让AI生成generateStandard=true的类型；WEIGHT模式保留所有启用类型以获取类型权重
            config = ReviewConfig.fromJson(params.getReviewConfig());
            boolean weightMode = config.isWeightMode();
            List<ReviewTypeConfig> aiTypes = getAiGeneratedTypes(config, weightMode);
            enabledTypes = aiTypes.stream()
                    .map(t -> ReviewType.fromCode(t.getReviewType()).getLabel())
                    .collect(Collectors.joining("、"));
            if (!weightMode) {
                BigDecimal manualScore = sumManualScore(config);
                aiScoreTotal = BigDecimal.valueOf(100).subtract(manualScore);
                if (aiScoreTotal.compareTo(BigDecimal.ZERO) < 0) {
                    aiScoreTotal = BigDecimal.ZERO;
                }
            }

            if (!StringUtils.hasText(enabledTypes)) {
                log.info("无启用的评审类型，跳过AI调用: taskId={}", task.getId());
                return "{\"reviewItems\":[]}";
            }
        } else {
            enabledTypes = ReviewType.getLabels();
        }

        // 按评分模式选择 system prompt 与 user prompt 构建方法
        boolean weightMode = config != null && config.isWeightMode();
        String systemPrompt = weightMode
                ? SystemPromptTemplates.REVIEW_ITEM_GENERATE_WEIGHT
                : SystemPromptTemplates.REVIEW_ITEM_GENERATE_SCORE;
        String userPrompt = weightMode
                ? PromptBuilder.buildReviewItemGenerateWeight(
                        params.getProjectName(),
                        params.getProjectType(),
                        params.getProjectCategory(),
                        params.getBudget(),
                        params.getRequirementContent(),
                        params.getReviewMethod(),
                        enabledTypes)
                : PromptBuilder.buildReviewItemGenerateScore(
                        params.getProjectName(),
                        params.getProjectType(),
                        params.getProjectCategory(),
                        params.getBudget(),
                        params.getRequirementContent(),
                        params.getReviewMethod(),
                        enabledTypes,
                        formatScore(aiScoreTotal));

        // 路由到合适的模型
        RoutedChatClient routedClient = modelRouter.routeWithInfo(AiTaskType.REVIEW_ITEM_GENERATE);

        // 同步调用并记录响应
        String aiOutput = aiCallRecorder.callAndRecord(routedClient.chatClient(), systemPrompt,
                userPrompt, "GENERATION", task.getId(), task.getCreateId(), task.getFileIdList(),
                routedClient.modelName());

        // 提取并归一化JSON内容
        String jsonResult = normalizeReviewItemJson(aiOutput, task.getId());
        if (!isValidReviewItemsJson(jsonResult)) {
            log.warn("评审项生成结果JSON解析失败，尝试AI修复: taskId={}", task.getId());
            String repairedOutput = repairReviewItemJson(routedClient, aiOutput, task);
            jsonResult = normalizeReviewItemJson(repairedOutput, task.getId());
        }

        // 验证结果包含reviewItems字段
        if (!isValidReviewItemsJson(jsonResult)) {
            log.warn("评审项生成结果缺少reviewItems字段, taskId={}", task.getId());
            throw new AiErrorContentException("AI输出格式异常，请重试", jsonResult);
        }

        log.info("评审项生成完成: taskId={}", task.getId());
        return jsonResult;
    }

    private List<ReviewTypeConfig> getAiGeneratedTypes(ReviewConfig config, boolean weightMode) {
        if (weightMode) {
            return config.getEnabledTypes();
        }
        return config.getEnabledTypes().stream()
                .filter(ReviewTypeConfig::isGenerateStandard)
                .toList();
    }

    private BigDecimal sumManualScore(ReviewConfig config) {
        return config.getEnabledTypes().stream()
                .filter(type -> !type.isGenerateStandard())
                .filter(type -> isScoringType(type.getReviewType()))
                .map(type -> sumManualLeafScores(type.getManualItems()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumManualLeafScores(List<TemplateReviewItemConfig> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TemplateReviewItemConfig item : items) {
            if (item == null) {
                continue;
            }
            List<TemplateReviewItemConfig> children = item.getChildren();
            if (children != null && !children.isEmpty()) {
                total = total.add(sumManualLeafScores(children));
            } else if (item.getScore() != null) {
                total = total.add(item.getScore());
            }
        }
        return total;
    }

    private boolean isScoringType(String reviewType) {
        return ReviewType.TECHNICAL.getCode().equals(reviewType)
                || ReviewType.CREDIT.getCode().equals(reviewType)
                || ReviewType.COMMERCIAL.getCode().equals(reviewType);
    }

    private String formatScore(BigDecimal score) {
        return score.stripTrailingZeros().toPlainString();
    }

    private String repairReviewItemJson(RoutedChatClient routedClient, String aiOutput, AiTask task) {
        String repairPrompt = PromptBuilder.buildReviewItemJsonRepair(aiOutput);
        return aiCallRecorder.callAndRecord(routedClient.chatClient(), SystemPromptTemplates.REVIEW_ITEM_JSON_REPAIR,
                repairPrompt, "GENERATION", task.getId(), task.getCreateId(), null, routedClient.modelName());
    }

    private boolean isValidReviewItemsJson(String jsonResult) {
        try {
            JsonNode root = objectMapper.readTree(jsonResult);
            JsonNode reviewItems = root.get("reviewItems");
            return reviewItems != null && reviewItems.isArray();
        } catch (Exception e) {
            log.warn("校验评审项JSON失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将AI常见偏差格式归一化为 {"reviewItems": [...]}。
     */
    private String normalizeReviewItemJson(String aiOutput, Long taskId) {
        String extractedJson = resultParser.extractJson(aiOutput);
        try {
            JsonNode root = objectMapper.readTree(extractedJson);

            JsonNode reviewItems = findReviewItemsArray(root);
            if (reviewItems != null) {
                return toReviewItemsJson(reviewItems);
            }

            ArrayNode categoryItems = convertCategoryObject(root);
            if (categoryItems != null && !categoryItems.isEmpty()) {
                return toReviewItemsJson(categoryItems);
            }
        } catch (Exception e) {
            log.warn("评审项生成结果JSON归一化失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
        return extractedJson;
    }

    private JsonNode findReviewItemsArray(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return node;
        }
        if (!node.isObject()) {
            return null;
        }

        for (String fieldName : REVIEW_ITEM_ARRAY_FIELD_NAMES) {
            JsonNode candidate = node.get(fieldName);
            if (candidate == null) {
                continue;
            }
            if (candidate.isArray()) {
                return candidate;
            }
            JsonNode nested = findReviewItemsArray(candidate);
            if (nested != null) {
                return nested;
            }
        }

        for (String fieldName : REVIEW_ITEM_WRAPPER_FIELD_NAMES) {
            JsonNode nested = findReviewItemsArray(node.get(fieldName));
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private ArrayNode convertCategoryObject(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }

        ArrayNode reviewItems = objectMapper.createArrayNode();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!REVIEW_CATEGORY_NAMES.contains(field.getKey()) || !field.getValue().isArray()) {
                continue;
            }

            ObjectNode category = objectMapper.createObjectNode();
            category.put("name", field.getKey());
            category.put("level", 1);
            category.set("children", field.getValue());
            reviewItems.add(category);
        }
        return reviewItems;
    }

    private String toReviewItemsJson(JsonNode reviewItems) throws Exception {
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.set("reviewItems", reviewItems);
        return objectMapper.writeValueAsString(normalized);
    }

}
