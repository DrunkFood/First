package com.jy.eleaitender.ai.processor.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.service.FileContentService;
import com.jy.eleaitender.common.dto.ai.DetectionParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 检测引擎（编排器）
 * 根据任务类型分发到具体的检测器
 */
@Slf4j
@Component
public class DetectionEngine {

    @Autowired
    private FileContentService fileContentService;

    @Autowired
    private SensitiveWordDetector sensitiveWordDetector;

    @Autowired
    private TypoDetector typoDetector;

    @Autowired
    private PolicyReviewDetector policyReviewDetector;

    @Autowired
    private FormatCheckDetector formatCheckDetector;

    @Autowired
    private GenerateResultParser resultParser;

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

        StringJoiner contentJoiner = new StringJoiner("\n\n");

        DetectionParams params = resultParser.parseParams(task.getRequestParams(), DetectionParams.class);
        String content = params.getContent();
        if (StringUtils.hasText(content)) {
            contentJoiner.add(content);
        }
        Long contentFileId = params.getContentFileId();
        if (contentFileId != null) {
            String extractContent = fileContentService.extractContent(contentFileId);
            if (StringUtils.hasText(extractContent)) {
                contentJoiner.add(extractContent);
            }
        }

        // 分发到具体检测器，传递任务ID和用户ID用于响应记录
        BaseDetector detector = resolveDetector(taskType);

        // 如果检测器需要文件ID列表，则从任务中获取
        List<String> fileIdList = null;
        if (detector.needFileFlag()) {
            fileIdList = task.getFileIdList();
        }

        if (taskType == AiTaskType.DETECTION_POLICY_REVIEW && !hasFileIds(fileIdList)) {
            log.info("未选择政策文件，跳过政策文件审查AI调用: taskId={}", task.getId());
            BaseDetector.DetectionResult skipped = new BaseDetector.DetectionResult();
            skipped.setScore(100);
            return toJson(skipped);
        }

        BaseDetector.DetectionResult result = detector.detect(contentJoiner.toString(),
                task.getId(), task.getCreateId(), fileIdList);

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

    private boolean hasFileIds(List<String> fileIds) {
        return fileIds != null && fileIds.stream().anyMatch(StringUtils::hasText);
    }

    private String toJson(BaseDetector.DetectionResult result) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "issues", result.getIssues(),
                    "score", result.getScore()
            ));
        } catch (Exception e) {
            log.warn("序列化检测结果失败: {}", e.getMessage(), e);
            return "{\"issues\": [], \"score\": 0, \"error\": \"结果序列化失败\"}";
        }
    }
}
