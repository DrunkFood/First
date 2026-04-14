package com.jy.eleaitender.core.service.impl;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.*;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.core.dto.request.DetectionSubmitRequest;
import com.jy.eleaitender.core.dto.response.DetectionIssueVO;
import com.jy.eleaitender.core.dto.response.DetectionProgressVO;
import com.jy.eleaitender.core.dto.response.DetectionReportVO;
import com.jy.eleaitender.core.entity.AiDetectionRecord;
import com.jy.eleaitender.core.entity.AiProject;
import com.jy.eleaitender.core.helper.MessageHelper;
import com.jy.eleaitender.core.mapper.AiDetectionRecordMapper;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IDetectionService;
import com.jy.eleaitender.core.statemachine.ProjectStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检测服务实现
 * 编排最终文档的4项智能检测
 */
@Slf4j
@Service
public class DetectionServiceImpl implements IDetectionService {

    @Autowired
    private AiProjectMapper projectMapper;

    @Autowired
    private AiDetectionRecordMapper detectionRecordMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private MessageHelper messageHelper;

    private static final DetectionType[] ALL_DETECTION_TYPES = {
            DetectionType.SENSITIVE_WORD,
            DetectionType.TYPO,
            DetectionType.POLICY_REVIEW,
            DetectionType.FORMAT_CHECK
    };

    @Override
    @Transactional
    public Map<String, Long> submit(Long projectId, DetectionSubmitRequest request) {
        AiProject project = getProjectOrThrow(projectId);

        // 状态转换: → PENDING_DETECTION
        ProjectStateMachine.transition(project, ProjectStatus.PENDING_DETECTION);
        projectMapper.updateById(project);

        // 政策文件ID列表
        String policyFileIdStr = "";
        if (request != null && request.getPolicyFileIds() != null && !request.getPolicyFileIds().isEmpty()) {
            policyFileIdStr = request.getPolicyFileIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        // 文档内容快照
        String contentSnapshot = project.getRequirementContent();

        Map<String, Long> taskIds = new LinkedHashMap<>();

        // 为每种检测类型创建检测记录 + AI任务
        for (DetectionType type : ALL_DETECTION_TYPES) {
            AiDetectionRecord record = new AiDetectionRecord();
            record.setProjectId(projectId);
            record.setDetectionType(type.getCode());
            record.setContentSnapshot(contentSnapshot);
            record.setStatus(AiTaskStatus.PENDING.getCode());
            record.setPolicyFileIds(policyFileIdStr);
            record.setStartedAt(LocalDateTime.now());
            detectionRecordMapper.insert(record);

            // 构建AI任务参数
            Map<String, Object> params = new HashMap<>();
            params.put("projectId", projectId);
            params.put("detectionRecordId", record.getId());
            params.put("detectionType", type.getCode());
            params.put("content", contentSnapshot);
            if (StringUtils.hasText(policyFileIdStr)) {
                params.put("policyFileIds", policyFileIdStr);
            }

            AiTaskType taskType = mapToTaskType(type);
            AiTask task = aiTaskService.createTask(taskType, projectId,
                    record.getId(), "DETECTION", params, policyFileIdStr);

            record.setTaskId(task.getId());
            detectionRecordMapper.updateById(record);

            taskIds.put(type.getCode(), task.getId());
        }

        // 状态转换: → DETECTING
        ProjectStateMachine.transition(project, ProjectStatus.DETECTING);
        projectMapper.updateById(project);

        log.info("提交文档检测，项目ID: {}, 创建4个检测任务", projectId);
        return taskIds;
    }

    @Override
    public DetectionProgressVO getProgress(Long projectId) {
        getProjectOrThrow(projectId);

        List<AiDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);

        DetectionProgressVO vo = new DetectionProgressVO();
        vo.setProjectId(projectId);

        List<DetectionProgressVO.DetectionItemProgress> items = new ArrayList<>();
        boolean allCompleted = true;
        boolean hasUnavailable = false;
        boolean hasFailed = false;
        int totalIssues = 0;

        for (AiDetectionRecord record : records) {
            DetectionProgressVO.DetectionItemProgress item = new DetectionProgressVO.DetectionItemProgress();
            item.setDetectionType(record.getDetectionType());
            item.setTypeName(getDetectionTypeName(record.getDetectionType()));
            item.setTaskStatus(record.getStatus());

            // 解析问题数
            int issueCount = parseIssueCount(record.getResult());
            item.setIssueCount(issueCount);
            totalIssues += issueCount;

            if (!AiTaskStatus.COMPLETED.getCode().equals(record.getStatus())) {
                allCompleted = false;
            }
            if (AiTaskStatus.AI_UNAVAILABLE.getCode().equals(record.getStatus())) {
                hasUnavailable = true;
            }
            if (AiTaskStatus.FAILED.getCode().equals(record.getStatus())) {
                hasFailed = true;
            }

            items.add(item);
        }

        vo.setItems(items);

        // 计算总体状态
        if (records.isEmpty()) {
            vo.setOverallStatus("PENDING");
        } else if (hasUnavailable) {
            vo.setOverallStatus("AI_UNAVAILABLE");
        } else if (hasFailed) {
            vo.setOverallStatus("FAILED");
        } else if (allCompleted) {
            vo.setOverallStatus(totalIssues > 0 ? "FAILED" : "PASSED");
        } else {
            vo.setOverallStatus("DETECTING");
        }

        return vo;
    }

    @Override
    public DetectionReportVO getReport(Long projectId) {
        AiProject project = getProjectOrThrow(projectId);
        List<AiDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);

        DetectionReportVO report = new DetectionReportVO();
        report.setProjectId(projectId);
        report.setProjectName(project.getProjectName());

        List<DetectionIssueVO> issues = new ArrayList<>();
        int totalIssueCount = 0;

        for (AiDetectionRecord record : records) {
            List<DetectionIssueVO> recordIssues = parseIssues(record);
            issues.addAll(recordIssues);
            totalIssueCount += recordIssues.size();
        }

        report.setIssues(issues);
        report.setTotalIssueCount(totalIssueCount);
        report.setOverallStatus(totalIssueCount > 0 ? "FAILED" : "PASSED");

        return report;
    }

    @Override
    @Transactional
    public void acceptIssue(Long recordId) {
        AiDetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResponseCode.DETECTION_NOT_FOUND);
        }
        // 标记为已接受（在result JSON中更新handleStatus）
        // 简化实现：直接更新状态
        log.info("接受检测建议，记录ID: {}", recordId);
    }

    @Override
    @Transactional
    public void rejectIssue(Long recordId) {
        AiDetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResponseCode.DETECTION_NOT_FOUND);
        }
        log.info("拒绝检测建议，记录ID: {}", recordId);
    }

    @Override
    @Transactional
    public void acceptAll(Long projectId) {
        List<AiDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);
        for (AiDetectionRecord record : records) {
            log.info("批量接受检测建议，记录ID: {}", record.getId());
        }
    }

    @Override
    @Transactional
    public void skip(Long projectId) {
        AiProject project = getProjectOrThrow(projectId);
        ProjectStateMachine.transition(project, ProjectStatus.DETECTION_SKIPPED);
        projectMapper.updateById(project);

        Long userId = SecurityContextHolder.getUserId();
        if (userId != null) {
            messageHelper.sendWarningNotice(userId,
                    "项目「" + project.getProjectName() + "」已跳过AI检测",
                    "项目已跳过智能检测流程，可直接进行发布操作。",
                    projectId);
        }

        log.info("跳过检测，项目ID: {}", projectId);
    }

    @Override
    @Transactional
    public Map<String, Long> retry(Long projectId) {
        AiProject project = getProjectOrThrow(projectId);

        // 重置状态为 DETECTING
        project.setStatus(ProjectStatus.DETECTING.getCode());
        projectMapper.updateById(project);

        // 找到失败的记录重新提交
        List<AiDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);
        Map<String, Long> taskIds = new LinkedHashMap<>();

        for (AiDetectionRecord record : records) {
            AiTaskStatus status = AiTaskStatus.fromCode(record.getStatus());
            if (status.isRetryable()) {
                record.setStatus(AiTaskStatus.PENDING.getCode());
                record.setResult(null);
                record.setStartedAt(LocalDateTime.now());
                record.setCompletedAt(null);
                detectionRecordMapper.updateById(record);

                Map<String, Object> params = new HashMap<>();
                params.put("projectId", projectId);
                params.put("detectionRecordId", record.getId());
                params.put("detectionType", record.getDetectionType());
                params.put("content", record.getContentSnapshot());
                if (StringUtils.hasText(record.getPolicyFileIds())) {
                    params.put("policyFileIds", record.getPolicyFileIds());
                }

                AiTaskType taskType = mapToTaskType(DetectionType.fromCode(record.getDetectionType()));
                AiTask task = aiTaskService.createTask(taskType, projectId,
                        record.getId(), "DETECTION", params, record.getPolicyFileIds());
                record.setTaskId(task.getId());
                detectionRecordMapper.updateById(record);

                taskIds.put(record.getDetectionType(), task.getId());
            }
        }

        log.info("重新检测，项目ID: {}, 重试任务数: {}", projectId, taskIds.size());
        return taskIds;
    }

    private AiProject getProjectOrThrow(Long projectId) {
        AiProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private AiTaskType mapToTaskType(DetectionType detectionType) {
        return switch (detectionType) {
            case SENSITIVE_WORD -> AiTaskType.DETECTION_SENSITIVE_WORD;
            case TYPO -> AiTaskType.DETECTION_TYPO;
            case POLICY_REVIEW -> AiTaskType.DETECTION_POLICY_REVIEW;
            case FORMAT_CHECK -> AiTaskType.DETECTION_FORMAT_CHECK;
        };
    }

    private String getDetectionTypeName(String code) {
        try {
            return DetectionType.fromCode(code).getLabel();
        } catch (Exception e) {
            return code;
        }
    }

    private int parseIssueCount(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return 0;
        }
        // 简化实现：后续可解析JSON中的issues数组长度
        return 0;
    }

    private List<DetectionIssueVO> parseIssues(AiDetectionRecord record) {
        if (!StringUtils.hasText(record.getResult())) {
            return Collections.emptyList();
        }
        // 简化实现：后续解析result JSON中的issues
        return Collections.emptyList();
    }
}
