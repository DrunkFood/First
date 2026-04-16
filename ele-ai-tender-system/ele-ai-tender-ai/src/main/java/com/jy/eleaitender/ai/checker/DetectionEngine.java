package com.jy.eleaitender.ai.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.dto.response.DetectionIssueVO;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 检测引擎（编排器）
 * 根据任务类型分发到具体的检测器
 */
@Slf4j
@Component
public class DetectionEngine {

    @Autowired
    private SensitiveWordDetector sensitiveWordDetector;

    @Autowired
    private TypoDetector typoDetector;

    @Autowired
    private PolicyReviewDetector policyReviewDetector;

    @Autowired
    private FormatCheckDetector formatCheckDetector;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 根据任务类型执行检测
     *
     * @param task AI任务
     * @return JSON结果字符串 {"issues": [...], "score": 85}
     */
    public String detect(AiTask task) {
        AiTaskType taskType = AiTaskType.fromCode(task.getTaskType());
        log.info("开始检测: taskId={}, type={}", task.getId(), taskType.getLabel());

        Map<String, Object> params = parseParams(task.getRequestParams());
        String content = getString(params, "content");

        // 分发到具体检测器，传递任务ID和用户ID用于响应记录
        BaseDetector detector = resolveDetector(taskType);
        BaseDetector.DetectionResult result = detector.detect(content, params,
                task.getId(), task.getCreateId());

        log.info("检测完成: taskId={}, type={}, issueCount={}, score={}",
                task.getId(), taskType.getLabel(), result.getIssues().size(), result.getScore());

        return toJson(result);
    }

    /**
     * 根据任务类型获取对应的检测器
     */
    private BaseDetector resolveDetector(AiTaskType taskType) {
        return switch (taskType) {
            case DETECTION_SENSITIVE_WORD -> sensitiveWordDetector;
            case DETECTION_TYPO -> typoDetector;
            case DETECTION_POLICY_REVIEW -> policyReviewDetector;
            case DETECTION_FORMAT_CHECK -> formatCheckDetector;
            default -> throw new IllegalArgumentException("非检测类型任务: " + taskType);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParams(String requestParams) {
        try {
            return objectMapper.readValue(requestParams, Map.class);
        } catch (Exception e) {
            log.warn("解析requestParams失败: {}", requestParams, e);
            return Map.of();
        }
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    private String toJson(BaseDetector.DetectionResult result) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "issues", result.getIssues(),
                    "score", result.getScore()
            ));
        } catch (Exception e) {
            return "{\"issues\": [], \"score\": 0, \"error\": \"结果序列化失败\"}";
        }
    }
}
