package com.jy.eleaitender.ai.processor.checker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.dto.response.DetectionIssueVO;
import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.enums.AiUsageScenario;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测器基类
 * 提供模型调用和结果解析的通用逻辑
 */
@Slf4j
public abstract class BaseDetector {

    @Autowired
    protected ModelRouter modelRouter;

    @Autowired
    protected GenerateResultParser resultParser;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AiCallRecorder aiCallRecorder;

    /**
     * 执行检测（带任务ID和用户ID，用于响应记录）
     *
     * @param content 待检测文本
     * @param taskId  关联AI任务ID
     * @param userId  用户ID（取自ai_task.create_id）
     * @param fileIds 关联文件ID列表
     * @return 检测结果
     */
    public DetectionResult detect(String content, Long taskId, Long userId, List<String> fileIds) {
        try {
            String systemPrompt = getSystemPrompt();
            String userPrompt = buildUserPrompt(content);

            ChatClient client = modelRouter.route(AiUsageScenario.DETECTION);

            String aiOutput = aiCallRecorder.callAndRecord(client, systemPrompt, userPrompt,
                    "DETECTION", taskId, userId, fileIds);

            return parseDetectionResult(aiOutput);
        } catch (Exception e) {
            log.error("{}检测失败: {}", getDetectionType(), e.getMessage(), e);
            DetectionResult result = new DetectionResult();
            result.setIssues(List.of());
            result.setScore(0);
            result.setError(e.getMessage());
            return result;
        }
    }

    protected abstract Boolean needFileFlag();

    /**
     * 获取检测类型名称
     */
    protected abstract String getDetectionType();

    /**
     * 获取System Prompt
     */
    protected abstract String getSystemPrompt();

    /**
     * 构建User Prompt
     */
    protected abstract String buildUserPrompt(String content);

    /**
     * 解析AI返回的检测结果
     */
    protected DetectionResult parseDetectionResult(String aiOutput) {
        DetectionResult result = new DetectionResult();
        try {
            String json = resultParser.extractJson(aiOutput);
            JsonNode root = objectMapper.readTree(json);

            // 解析issues
            if (root.has("issues")) {
                List<DetectionIssueVO> issues = objectMapper.convertValue(root.get("issues"), new TypeReference<>() {
                });
                // 为每个issue设置检测类型
                issues.forEach(issue -> issue.setDetectionType(getDetectionType()));
                result.setIssues(issues);
            } else {
                result.setIssues(new ArrayList<>());
            }

            // 解析评分
            if (root.has("score")) {
                result.setScore(root.get("score").asInt(100));
            } else {
                result.setScore(100);
            }
        } catch (Exception e) {
            log.warn("解析检测结果失败, type={}: {}", getDetectionType(), e.getMessage());
            result.setIssues(new ArrayList<>());
            result.setScore(0);
            result.setError("检测结果解析失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 检测结果内部类
     */
    @Data
    public static class DetectionResult {
        private List<DetectionIssueVO> issues = new ArrayList<>();
        private int score = 100;
        private String error;
    }
}
