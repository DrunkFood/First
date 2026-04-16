package com.jy.eleaitender.core.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiDetectionRecord;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.common.entity.core.AiReviewItem;
import com.jy.eleaitender.common.enums.AiTaskStatus;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ProjectPhase;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.core.helper.MessageHelper;
import com.jy.eleaitender.core.mapper.AiDetectionRecordMapper;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.mapper.AiReviewItemMapper;
import com.jy.eleaitender.core.statemachine.ProjectStateMachine;
import com.jy.eleaitender.core.util.DetectionResultParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI任务结果同步处理器
 * 按任务类型将ai_task.result同步到对应业务表
 */
@Slf4j
@Component
public class AiTaskResultSyncHandler {

    @Autowired
    private AiRequirementMapper requirementMapper;

    @Autowired
    private AiReviewItemMapper reviewItemMapper;

    @Autowired
    private AiDetectionRecordMapper detectionRecordMapper;

    @Autowired
    private AiProjectMapper projectMapper;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 按任务类型分发同步逻辑
     */
    public void sync(AiTask task) {
        // SKIPPED 任务不需要同步业务数据
        if (AiTaskStatus.SKIPPED.getCode().equals(task.getStatus())) {
            log.debug("跳过SKIPPED任务的同步: id={}", task.getId());
            return;
        }

        // 防护：未知任务类型不阻塞同步流程
        AiTaskType taskType;
        try {
            taskType = AiTaskType.fromCode(task.getTaskType());
        } catch (IllegalArgumentException e) {
            log.warn("未知任务类型，跳过同步: taskType={}", task.getTaskType());
            return;
        }

        switch (taskType) {
            case REQUIREMENT_GENERATE -> syncRequirement(task);
            case REVIEW_ITEM_GENERATE -> syncReviewItems(task);
            case DETECTION_SENSITIVE_WORD, DETECTION_TYPO,
                 DETECTION_POLICY_REVIEW, DETECTION_FORMAT_CHECK -> syncDetection(task);
            // TEXT_OPTIMIZE 通过SSE直接返回，无需同步到业务表
            default -> log.debug("无需同步的任务类型: {}", taskType);
        }
    }

    // ========== 需求生成同步 ==========

    private void syncRequirement(AiTask task) {
        Long requirementId = task.getBizId();
        AiRequirement requirement = requirementMapper.selectById(requirementId);
        if (requirement == null) {
            log.warn("需求不存在，跳过同步: requirementId={}", requirementId);
            return;
        }

        if (!AiTaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            log.info("需求生成任务非成功状态，不更新内容: requirementId={}, taskStatus={}",
                    requirementId, task.getStatus());
            return;
        }

        String content = parseContentFromResult(task.getResult());
        if (content == null) {
            log.warn("解析需求生成结果为空，跳过同步: requirementId={}", requirementId);
            return;
        }

        requirement.setContent(content);
        requirement.setStatus("COMPLETED");
        requirement.setProgress(100);
        requirementMapper.updateById(requirement);
        log.info("同步需求内容成功: requirementId={}", requirementId);
    }

    private String parseContentFromResult(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode contentNode = root.get("content");
            return contentNode != null && !contentNode.isNull() ? contentNode.asText() : null;
        } catch (Exception e) {
            log.error("解析需求生成结果失败", e);
            return null;
        }
    }

    // ========== 评审项生成同步 ==========

    @Transactional
    private void syncReviewItems(AiTask task) {
        Long projectId = task.getBizId();

        if (!AiTaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            log.info("评审项生成任务非成功状态，跳过: projectId={}, taskStatus={}",
                    projectId, task.getStatus());
            return;
        }

        List<AiReviewItem> items = parseReviewItemsFromResult(task.getResult(), projectId);
        if (items.isEmpty()) {
            log.warn("评审项生成结果为空，跳过同步: projectId={}", projectId);
            return;
        }

        // 先删除该项目下已有评审项（AI全量生成模式）
        List<AiReviewItem> existing = reviewItemMapper.selectByProjectId(projectId);
        for (AiReviewItem existingItem : existing) {
            reviewItemMapper.deleteById(existingItem.getId());
        }

        // 按 level 排序确保父节点先插入
        items.sort((a, b) -> {
            int levelA = a.getLevel() != null ? a.getLevel() : 1;
            int levelB = b.getLevel() != null ? b.getLevel() : 1;
            return Integer.compare(levelA, levelB);
        });

        for (AiReviewItem item : items) {
            reviewItemMapper.insert(item);
        }

        log.info("同步评审项成功: projectId={}, count={}", projectId, items.size());
    }

    private List<AiReviewItem> parseReviewItemsFromResult(String resultJson, Long projectId) {
        if (!StringUtils.hasText(resultJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode itemsNode = root.get("reviewItems");
            if (itemsNode == null || !itemsNode.isArray()) {
                return List.of();
            }

            java.util.List<AiReviewItem> items = new java.util.ArrayList<>();
            int sortOrder = 0;
            for (JsonNode itemNode : itemsNode) {
                AiReviewItem item = new AiReviewItem();
                item.setProjectId(projectId);
                item.setItemName(getText(itemNode, "itemName"));
                item.setItemContent(getText(itemNode, "itemContent"));
                item.setLevel(getInt(itemNode, "level", 1));
                item.setSortOrder(sortOrder++);
                item.setReviewType(getText(itemNode, "reviewType"));
                item.setScore(getDecimal(itemNode, "score"));
                item.setMaxScore(getDecimal(itemNode, "maxScore"));
                item.setWeight(getDecimal(itemNode, "weight"));
                item.setSubjectivity(getText(itemNode, "subjectivity"));
                item.setIsRequired(getInt(itemNode, "isRequired", 1));
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            log.error("解析评审项结果失败", e);
            return List.of();
        }
    }

    // ========== 检测任务同步 ==========

    private void syncDetection(AiTask task) {
        Long recordId = task.getBizId();
        AiDetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null) {
            log.warn("检测记录不存在，跳过同步: recordId={}", recordId);
            return;
        }

        // 同步状态
        record.setStatus(task.getStatus());

        // COMPLETED: 同步结果和完成时间
        if (AiTaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            record.setResult(task.getResult());
            record.setCompletedAt(LocalDateTime.now());
        } else if (AiTaskStatus.FAILED.getCode().equals(task.getStatus())
                || AiTaskStatus.AI_UNAVAILABLE.getCode().equals(task.getStatus())
                || AiTaskStatus.SKIPPED.getCode().equals(task.getStatus())) {
            record.setCompletedAt(LocalDateTime.now());
        }

        detectionRecordMapper.updateById(record);
        log.info("同步检测记录成功: recordId={}, status={}", recordId, task.getStatus());

        // 检查该项目所有检测记录是否都已完成，若是则更新项目状态
        checkAndUpdateProjectDetectionStatus(record.getProjectId());
    }

    private void checkAndUpdateProjectDetectionStatus(Long projectId) {
        List<AiDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);
        if (records.isEmpty()) {
            return;
        }

        // 是否所有检测记录都已终态
        boolean allTerminal = records.stream().allMatch(r -> {
            AiTaskStatus s = AiTaskStatus.fromCode(r.getStatus());
            return s.isTerminal();
        });
        if (!allTerminal) {
            return;
        }

        AiProject project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }

        // 只有当前状态是 DETECTING 才转换
        if (!ProjectStatus.DETECTING.getCode().equals(project.getStatus())) {
            return;
        }

        // 判断检测结果
        boolean hasTaskFailed = records.stream().anyMatch(r ->
                AiTaskStatus.FAILED.getCode().equals(r.getStatus())
                        || AiTaskStatus.AI_UNAVAILABLE.getCode().equals(r.getStatus()));

        int totalIssues = records.stream()
                .mapToInt(r -> DetectionResultParser.parseIssueCount(r.getResult()))
                .sum();

        try {
            if (hasTaskFailed || totalIssues > 0) {
                ProjectStateMachine.transition(project, ProjectStatus.DETECTION_FAILED);
            } else {
                ProjectStateMachine.transition(project, ProjectStatus.DETECTION_PASSED);
            }
            projectMapper.updateById(project);

            // 联动阶段推进：检测通过时，标记检测阶段完成
            if (ProjectStatus.DETECTION_PASSED.getCode().equals(project.getStatus())
                    || ProjectStatus.DETECTION_SKIPPED.getCode().equals(project.getStatus())) {
                if (project.getCurrentPhase() < ProjectPhase.DETECTION.getCode()) {
                    project.setCurrentPhase(ProjectPhase.DETECTION.getCode());
                    project.setProgress(ProjectPhase.DETECTION.getProgressPercent());
                    projectMapper.updateById(project);
                }
            }

            // 发送检测完成通知
            Long userId = project.getCreateId();
            if (userId != null) {
                String statusLabel = ProjectStatus.fromCode(project.getStatus()).getLabel();
                String detail = totalIssues > 0 ? "，共发现" + totalIssues + "个问题" : "";
                messageHelper.sendDetectionNotice(userId,
                        "项目「" + project.getProjectName() + "」检测完成",
                        "检测结果：" + statusLabel + detail,
                        projectId);
            }

            log.info("项目检测状态流转: projectId={}, newStatus={}", projectId, project.getStatus());
        } catch (Exception e) {
            log.error("更新项目检测状态失败: projectId={}", projectId, e);
        }
    }

    // ========== JSON 解析辅助 ==========

    private String getText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    private int getInt(JsonNode node, String field, int defaultValue) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && fieldNode.isNumber() ? fieldNode.asInt() : defaultValue;
    }

    private BigDecimal getDecimal(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && fieldNode.isNumber()) {
            return fieldNode.decimalValue();
        }
        return null;
    }
}
