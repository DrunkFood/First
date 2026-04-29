package com.jy.eleaitender.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jy.eleaitender.common.dto.FixReplacement;
import com.jy.eleaitender.common.dto.LocationRefVO;
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import com.jy.eleaitender.common.enums.DetectionType;
import com.jy.eleaitender.common.util.TextNormalizeUtil;
import com.jy.eleaitender.core.dto.response.DetectionIssueVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jy.eleaitender.common.util.JsonUtil.getInt;
import static com.jy.eleaitender.common.util.JsonUtil.getText;

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
                vo.setDetectionType(getText(issueNode, "detectionType", record.getDetectionType()));
                vo.setTypeName(getTypeName(issueNode, record.getDetectionType()));
                vo.setLocation(getText(issueNode, "position", ""));
                vo.setOriginal(getText(issueNode, "original", ""));
                vo.setTargeted(getText(issueNode, "targeted", ""));
                vo.setDescription(buildDescription(issueNode));
                vo.setSuggestion(getText(issueNode, "suggestion", ""));
                vo.setSeverity(getText(issueNode, "severity", "MEDIUM"));
                vo.setHandleStatus(getInt(issueNode, "handleStatus", 0));
                vo.setIssueIndex(i);
                vo.setPolicyReference(getText(issueNode, "policyReference", null));
                vo.setRuleViolated(getText(issueNode, "ruleViolated", null));
                // 解析locationRef
                JsonNode locationRefNode = issueNode.get("locationRef");
                if (locationRefNode != null && !locationRefNode.isNull()) {
                    LocationRefVO locationRef = new LocationRefVO();
                    locationRef.setType(getText(locationRefNode, "type", null));
                    locationRef.setElementIndex(locationRefNode.has("elementIndex")
                            ? locationRefNode.get("elementIndex").asInt() : null);
                    locationRef.setTableIndex(locationRefNode.has("tableIndex")
                            ? locationRefNode.get("tableIndex").asInt() : null);
                    locationRef.setRowIndex(locationRefNode.has("rowIndex")
                            ? locationRefNode.get("rowIndex").asInt() : null);
                    locationRef.setCellIndex(locationRefNode.has("cellIndex")
                            ? locationRefNode.get("cellIndex").asInt() : null);
                    vo.setLocationRef(locationRef);
                }
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
     * @param resultJson   原始result JSON
     * @param issueIndex   issues数组中的索引
     * @param handleStatus 新的handleStatus值 (0-未处理 1-已接受 2-已拒绝 3-未找到)
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
        String code = getText(issueNode, "detectionType", defaultDetectionType);
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
        String original = getText(issueNode, "original", "");
        String reason = getText(issueNode, "reason", "");
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
                return getText(issuesNode.get(issueIndex), field, "");
            }
            return "";
        } catch (Exception e) {
            log.error("获取检测问题字段失败, issueIndex={}, field={}", issueIndex, field, e);
            return "";
        }
    }

    /**
     * 从检测记录列表中提取所有已接受问题(handleStatus=1)的原文-替换对
     *
     * @param records 检测记录列表
     * @return 替换对列表
     */
    public static List<FixReplacement> parseAcceptedReplacements(List<TbDetectionRecord> records) {
        List<FixReplacement> replacements = new ArrayList<>();
        for (TbDetectionRecord record : records) {
            if (!StringUtils.hasText(record.getResult())) continue;
            try {
                JsonNode root = MAPPER.readTree(record.getResult());
                JsonNode issuesNode = root.get("issues");
                if (issuesNode == null || !issuesNode.isArray()) continue;
                for (JsonNode issue : issuesNode) {
                    int handleStatus = getInt(issue, "handleStatus", 0);
                    if (handleStatus == 1) {
                        String original = getText(issue, "original", "");
                        String targeted = getText(issue, "targeted", "");
                        if (StringUtils.hasText(original) && StringUtils.hasText(targeted)
                                && !original.equals(targeted)) {
                            FixReplacement rep = new FixReplacement();
                            rep.setOriginal(original);
                            rep.setTargeted(targeted);
                            // 传递locationRef供精准定位
                            JsonNode locRefNode = issue.get("locationRef");
                            if (locRefNode != null && !locRefNode.isNull()) {
                                LocationRefVO locRef = new LocationRefVO();
                                locRef.setType(getText(locRefNode, "type", null));
                                locRef.setElementIndex(locRefNode.has("elementIndex")
                                        ? locRefNode.get("elementIndex").asInt() : null);
                                locRef.setTableIndex(locRefNode.has("tableIndex")
                                        ? locRefNode.get("tableIndex").asInt() : null);
                                locRef.setRowIndex(locRefNode.has("rowIndex")
                                        ? locRefNode.get("rowIndex").asInt() : null);
                                locRef.setCellIndex(locRefNode.has("cellIndex")
                                        ? locRefNode.get("cellIndex").asInt() : null);
                                rep.setLocationRef(locRef);
                            }
                            replacements.add(rep);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("解析检测替换对失败, recordId={}", record.getId(), e);
            }
        }
        return replacements;
    }

    /**
     * 检查是否存在未处理的问题(handleStatus=0)
     *
     * @param records 检测记录列表
     * @return true=存在未处理问题
     */
    public static boolean hasUnresolvedIssues(List<TbDetectionRecord> records) {
        for (TbDetectionRecord record : records) {
            if (!StringUtils.hasText(record.getResult())) continue;
            try {
                JsonNode root = MAPPER.readTree(record.getResult());
                JsonNode issuesNode = root.get("issues");
                if (issuesNode == null || !issuesNode.isArray()) continue;
                for (JsonNode issue : issuesNode) {
                    int handleStatus = getInt(issue, "handleStatus", 0);
                    if (handleStatus == 0) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.error("检查未处理问题失败, recordId={}", record.getId(), e);
            }
        }
        return false;
    }

    /**
     * 为检测结果JSON中的每个issue填充locationRef
     *
     * @param resultJson 原始result JSON
     * @param fullText   文档全文
     * @param segments   位置索引段列表
     * @return 填充后的JSON字符串
     */
    public static String fillLocationRefs(String resultJson, String fullText, List<Map<String, Object>> segments) {
        if (!StringUtils.hasText(resultJson) || !StringUtils.hasText(fullText)) {
            return resultJson;
        }
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(resultJson);
            JsonNode issuesNode = root.get("issues");
            if (issuesNode == null || !issuesNode.isArray()) {
                return resultJson;
            }

            // 构建offsets列表用于二分查找
            List<int[]> offsets = new ArrayList<>();
            for (Map<String, Object> seg : segments) {
                int offset = ((Number) seg.get("fullTextOffset")).intValue();
                offsets.add(new int[]{offset, ((Number) seg.get("elementIndex")).intValue()});
            }

            for (JsonNode issueNode : issuesNode) {
                // 跳过已有locationRef的issue
                if (issueNode.has("locationRef") && !issueNode.get("locationRef").isNull()) {
                    continue;
                }

                String original = getText(issueNode, "original", "");
                if (!StringUtils.hasText(original)) continue;

                int offset = fullText.indexOf(original);

                // 规范化匹配
                if (offset < 0) {
                    String normalizedFull = TextNormalizeUtil.normalize(fullText);
                    String normalizedOriginal = TextNormalizeUtil.normalize(original);
                    int normStart = normalizedFull.indexOf(normalizedOriginal);
                    if (normStart >= 0) {
                        offset = mapNormToRaw(fullText, normStart);
                    }
                }

                if (offset < 0) continue;

                // 二分查找segment
                int segIdx = findSegmentIndex(offsets, offset);
                if (segIdx < 0) continue;

                Map<String, Object> targetSeg = segments.get(segIdx);
                ObjectNode locationRef = MAPPER.createObjectNode();
                locationRef.put("type", (String) targetSeg.get("type"));
                locationRef.put("elementIndex", ((Number) targetSeg.get("elementIndex")).intValue());
                if ("table".equals(targetSeg.get("type"))) {
                    locationRef.put("tableIndex", ((Number) targetSeg.get("tableIndex")).intValue());
                    locationRef.put("rowIndex", ((Number) targetSeg.get("rowIndex")).intValue());
                    locationRef.put("cellIndex", ((Number) targetSeg.get("cellIndex")).intValue());
                }
                ((ObjectNode) issueNode).set("locationRef", locationRef);
            }

            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            log.error("填充locationRef失败", e);
            return resultJson;
        }
    }

    /**
     * 获取指定issue的locationRef
     */
    public static LocationRefVO parseLocationRef(String resultJson, int issueIndex) {
        if (!StringUtils.hasText(resultJson)) return null;
        try {
            JsonNode root = MAPPER.readTree(resultJson);
            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray()
                    && issueIndex >= 0 && issueIndex < issuesNode.size()) {
                JsonNode locationRefNode = issuesNode.get(issueIndex).get("locationRef");
                if (locationRefNode != null && !locationRefNode.isNull()) {
                    LocationRefVO ref = new LocationRefVO();
                    ref.setType(getText(locationRefNode, "type", null));
                    ref.setElementIndex(locationRefNode.has("elementIndex")
                            ? locationRefNode.get("elementIndex").asInt() : null);
                    ref.setTableIndex(locationRefNode.has("tableIndex")
                            ? locationRefNode.get("tableIndex").asInt() : null);
                    ref.setRowIndex(locationRefNode.has("rowIndex")
                            ? locationRefNode.get("rowIndex").asInt() : null);
                    ref.setCellIndex(locationRefNode.has("cellIndex")
                            ? locationRefNode.get("cellIndex").asInt() : null);
                    return ref;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("解析locationRef失败, issueIndex={}", issueIndex, e);
            return null;
        }
    }

    /**
     * 简化的规范化偏移映射：找到规范化文本中第normStart个字符在原始文本中的位置
     * 逻辑与 WordTextExtractor.buildNormToRawMapping 保持一致
     */
    private static int mapNormToRaw(String rawText, int normStart) {
        int rawIdx = 0;
        int normIdx = 0;
        while (rawIdx < rawText.length() && normIdx < normStart) {
            if (TextNormalizeUtil.isWhitespaceChar(rawText.charAt(rawIdx))) {
                rawIdx++;
                continue;
            }
            rawIdx++;
            normIdx++;
        }
        // 跳过 rawIdx 处的空白字符，定位到匹配子串的第一个非空白字符
        while (rawIdx < rawText.length() && TextNormalizeUtil.isWhitespaceChar(rawText.charAt(rawIdx))) {
            rawIdx++;
        }
        return rawIdx;
    }

    /**
     * 二分查找：根据fullTextOffset找到segment索引
     */
    private static int findSegmentIndex(List<int[]> offsets, int targetOffset) {
        int lo = 0, hi = offsets.size() - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (offsets.get(mid)[0] <= targetOffset) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }
}
