package com.jy.eleaitender.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import com.jy.eleaitender.common.enums.DetectionType;
import com.jy.eleaitender.core.dto.response.DetectionIssueVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 检测结果JSON解析工具类
 * 解析AI模块DetectionEngine.toJson()写入的result字段
 */
@Slf4j
public class DetectionResultParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DetectionResultParser() {
    }

    /**
     * 解析result JSON，返回问题列表
     */
    public static List<DetectionIssueVO> parseIssues(TbDetectionRecord record) {
        if (record == null || !StringUtils.hasText(record.getResult())) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = MAPPER.readTree(record.getResult());
            JsonNode issuesNode = root.get("issues");
            if (issuesNode == null || !issuesNode.isArray()) {
                return Collections.emptyList();
            }

            List<DetectionIssueVO> result = new ArrayList<>();
            for (int i = 0; i < issuesNode.size(); i++) {
                JsonNode issueNode = issuesNode.get(i);
                DetectionIssueVO vo = new DetectionIssueVO();
                vo.setRecordId(record.getId());
                vo.setDetectionType(getStringValue(issueNode, "detectionType", record.getDetectionType()));
                vo.setTypeName(getTypeName(issueNode, record.getDetectionType()));
                vo.setLocation(getStringValue(issueNode, "position", ""));
                vo.setDescription(buildDescription(issueNode));
                vo.setSuggestion(getStringValue(issueNode, "suggestion", ""));
                vo.setSeverity(getStringValue(issueNode, "severity", "MEDIUM"));
                vo.setHandleStatus(getIntValue(issueNode, "handleStatus", 0));
                vo.setIssueIndex(i);
                vo.setPolicyReference(getStringValue(issueNode, "policyReference", null));
                vo.setRuleViolated(getStringValue(issueNode, "ruleViolated", null));
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            log.error("解析检测结果JSON失败, recordId={}", record.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析score
     */
    public static int parseScore(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return 0;
        }
        try {
            JsonNode root = MAPPER.readTree(resultJson);
            JsonNode scoreNode = root.get("score");
            return scoreNode != null && scoreNode.isNumber() ? scoreNode.asInt() : 0;
        } catch (Exception e) {
            log.error("解析检测评分失败", e);
            return 0;
        }
    }

    /**
     * 解析issue数量
     */
    public static int parseIssueCount(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return 0;
        }
        try {
            JsonNode root = MAPPER.readTree(resultJson);
            JsonNode issuesNode = root.get("issues");
            return issuesNode != null && issuesNode.isArray() ? issuesNode.size() : 0;
        } catch (Exception e) {
            log.error("解析检测问题数量失败", e);
            return 0;
        }
    }

    /**
     * 更新指定issue的handleStatus
     *
     * @param resultJson    原始result JSON
     * @param issueIndex    issues数组中的索引
     * @param handleStatus  新的handleStatus值 (0-未处理 1-已接受 2-已拒绝)
     * @return 更新后的JSON字符串，失败返回原JSON
     */
    public static String updateIssueHandleStatus(String resultJson, int issueIndex, int handleStatus) {
        if (!StringUtils.hasText(resultJson)) {
            return resultJson;
        }
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(resultJson);
            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray() && issueIndex >= 0 && issueIndex < issuesNode.size()) {
                ((ObjectNode) issuesNode.get(issueIndex)).put("handleStatus", handleStatus);
                return MAPPER.writeValueAsString(root);
            }
            return null; // 越界时返回null，上层已判断 null 不写入
        } catch (Exception e) {
            log.error("更新检测问题处理状态失败, issueIndex={}", issueIndex, e);
            return null;
        }
    }

    /**
     * 批量接受所有issue
     *
     * @param resultJson 原始result JSON
     * @return 更新后的JSON字符串，失败返回原JSON
     */
    public static String acceptAllIssues(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return resultJson;
        }
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(resultJson);
            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray()) {
                for (JsonNode issue : issuesNode) {
                    JsonNode statusNode = issue.get("handleStatus");
                    if (statusNode == null || statusNode.asInt(-1) == 0) {
                        ((ObjectNode) issue).put("handleStatus", 1);
                    }
                }
                return MAPPER.writeValueAsString(root);
            }
            return resultJson;
        } catch (Exception e) {
            log.error("批量接受检测建议失败", e);
            return resultJson;
        }
    }

    private static String getTypeName(JsonNode issueNode, String defaultDetectionType) {
        String code = getStringValue(issueNode, "detectionType", defaultDetectionType);
        try {
            return DetectionType.fromCode(code).getLabel();
        } catch (Exception e) {
            return code;
        }
    }

    /**
     * 拼接 original + reason 作为 description
     */
    private static String buildDescription(JsonNode issueNode) {
        String original = getStringValue(issueNode, "original", "");
        String reason = getStringValue(issueNode, "reason", "");
        if (StringUtils.hasText(original) && StringUtils.hasText(reason)) {
            return original + "：" + reason;
        }
        if (StringUtils.hasText(original)) {
            return original;
        }
        return reason;
    }

    /**
     * 获取指定issue的指定字段值
     */
    public static String getIssueField(String resultJson, int issueIndex, String field) {
        if (!StringUtils.hasText(resultJson)) {
            return "";
        }
        try {
            JsonNode root = MAPPER.readTree(resultJson);
            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray()
                    && issueIndex >= 0 && issueIndex < issuesNode.size()) {
                return getStringValue(issuesNode.get(issueIndex), field, "");
            }
            return "";
        } catch (Exception e) {
            log.error("获取检测问题字段失败, issueIndex={}, field={}", issueIndex, field, e);
            return "";
        }
    }

    private static String getStringValue(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText(defaultValue) : defaultValue;
    }

    private static int getIntValue(JsonNode node, String field, int defaultValue) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && fieldNode.isNumber() ? fieldNode.asInt() : defaultValue;
    }
}
