package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jy.eleaitender.common.client.InternalFileServiceClient;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.dto.FixReplacement;
import com.jy.eleaitender.common.dto.LocationRefVO;
import com.jy.eleaitender.common.dto.ai.DetectionParams;
import com.jy.eleaitender.common.dto.response.WordFixResultVO;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.*;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.core.dto.request.DetectionSubmitRequest;
import com.jy.eleaitender.core.dto.response.*;
import com.jy.eleaitender.core.helper.MessageHelper;
import com.jy.eleaitender.core.mapper.SupPolicyFileMapper;
import com.jy.eleaitender.core.mapper.TbDetectionRecordMapper;
import com.jy.eleaitender.core.mapper.TbPolicyFileMapper;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IDetectionService;
import com.jy.eleaitender.core.service.IProjectVersionService;
import com.jy.eleaitender.core.statemachine.ProjectStateMachine;
import com.jy.eleaitender.core.util.DetectionResultParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
    private TbProjectMapper projectMapper;

    @Autowired
    private TbDetectionRecordMapper detectionRecordMapper;

    @Autowired
    private TbProjectReviewItemMapper reviewItemMapper;

    @Autowired
    private TbPolicyFileMapper policyFileMapper;

    @Autowired
    private SupPolicyFileMapper supPolicyFileMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private InternalFileServiceClient fileServiceClient;

    @Autowired
    private IProjectVersionService projectVersionService;

    private static final DetectionType[] ALL_DETECTION_TYPES = {
            DetectionType.SENSITIVE_WORD,
            DetectionType.TYPO,
            DetectionType.POLICY_REVIEW,
            DetectionType.FORMAT_CHECK
    };

    private static final String EMPTY_PASS_RESULT = "{\"issues\":[],\"score\":100}";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> submit(Long projectId, DetectionSubmitRequest request) {
        TbProject project = getProjectOrThrow(projectId);

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
        boolean hasPolicyFiles = hasPolicyFileIds(policyFileIdStr);

        Map<String, Long> taskIds = new LinkedHashMap<>();
        String detectionContent = buildDetectionContent(project, reviewItemMapper.selectByProjectId(projectId));

        // 为每种检测类型创建检测记录 + AI任务
        for (DetectionType type : ALL_DETECTION_TYPES) {
            if (type == DetectionType.POLICY_REVIEW && !hasPolicyFiles) {
                log.info("未选择政策文件，跳过政策文件审查: projectId={}", projectId);
                continue;
            }

            TbDetectionRecord record = new TbDetectionRecord();
            record.setProjectId(projectId);
            record.setDetectionType(type.getCode());
            record.setContentFileId(null);
            record.setContentSnapshot(detectionContent);
            record.setStatus(AiTaskStatus.PENDING.getCode());
            record.setPolicyFileIds(policyFileIdStr);
            record.setStartedAt(LocalDateTime.now());
            detectionRecordMapper.insert(record);

            // 构建AI任务参数
            DetectionParams params = new DetectionParams();
            params.setContent(detectionContent);

            AiTaskType taskType = AiTaskType.mapToTaskType(type);
            AiTask task = aiTaskService.createTask(taskType, projectId,
                    record.getId(), "DETECTION", params, policyFileIdStr);

            record.setTaskId(task.getId());
            detectionRecordMapper.updateById(record);

            taskIds.put(type.getCode(), task.getId());
        }

        // 状态转换: → DETECTING
        ProjectStateMachine.transition(project, ProjectStatus.DETECTING);
        projectMapper.updateById(project);

        log.info("提交文档检测，项目ID: {}, 创建{}个检测任务", projectId, taskIds.size());
        return taskIds;
    }

    @Override
    public DetectionProgressVO getProgress(Long projectId) {
        getProjectOrThrow(projectId);

        List<TbDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);

        DetectionProgressVO vo = new DetectionProgressVO();
        vo.setProjectId(projectId);

        List<DetectionProgressVO.DetectionItemProgress> items = new ArrayList<>();
        boolean allCompleted = true;
        boolean hasUnavailable = false;
        boolean hasFailed = false;
        int totalIssues = 0;

        for (TbDetectionRecord record : records) {
            DetectionProgressVO.DetectionItemProgress item = new DetectionProgressVO.DetectionItemProgress();
            item.setDetectionType(record.getDetectionType());
            item.setTypeName(getDetectionTypeName(record.getDetectionType()));
            item.setTaskStatus(record.getStatus());

            // 解析问题数
            int issueCount = DetectionResultParser.parseIssueCount(record.getResult());
            item.setIssueCount(issueCount);
            int score = DetectionResultParser.parseScore(record.getResult());
            item.setScore(java.math.BigDecimal.valueOf(score));
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
        TbProject project = getProjectOrThrow(projectId);
        List<TbDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);

        DetectionReportVO report = new DetectionReportVO();
        report.setProjectId(projectId);
        report.setProjectName(project.getProjectName());

        List<DetectionIssueVO> issues = new ArrayList<>();
        int totalIssueCount = 0;

        for (TbDetectionRecord record : records) {
            List<DetectionIssueVO> recordIssues = DetectionResultParser.parseIssues(record);
            issues.addAll(recordIssues);
            totalIssueCount += recordIssues.size();
        }

        report.setIssues(issues);
        report.setTotalIssueCount(totalIssueCount);
        report.setOverallStatus(totalIssueCount > 0 ? "FAILED" : "PASSED");

        // 填充检测文件信息
        report.setDetectionFiles(buildDetectionFiles(project, records));

        return report;
    }

    private List<DetectionFileInfoVO> buildDetectionFiles(TbProject project, List<TbDetectionRecord> records) {
        List<DetectionFileInfoVO> files = new ArrayList<>();

        // 主招标文件
        if (project.getGeneratedFileId() != null) {
            try {
                var fileInfo = fileServiceClient.info(project.getGeneratedFileId());
                if (fileInfo != null && fileInfo.getFileName() != null) {
                    DetectionFileInfoVO vo = new DetectionFileInfoVO();
                    vo.setFileId(project.getGeneratedFileId());
                    vo.setFileName(fileInfo.getFileName());
                    vo.setFileType("主招标文件");
                    files.add(vo);
                }
            } catch (Exception e) {
                log.error("获取主招标文件信息失败: fileId={}", project.getGeneratedFileId(), e);
            }
        }

        // 收集所有政策文件ID
        Set<Long> policyIds = new LinkedHashSet<>();
        for (TbDetectionRecord record : records) {
            if (!StringUtils.hasText(record.getPolicyFileIds())) continue;
            for (String idStr : record.getPolicyFileIds().split(",")) {
                try {
                    policyIds.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException e) {
                    log.warn("政策文件ID格式异常: '{}'", idStr.trim(), e);
                }
            }
        }

        // 批量查询政策文件名称
        if (!policyIds.isEmpty()) {
            Map<Long, String> nameMap = new LinkedHashMap<>();
            // 先查用户级
            policyFileMapper.selectBatchIds(policyIds)
                    .forEach(f -> nameMap.put(f.getId(), f.getFileName()));
            // 再查系统级（未被用户级覆盖的）
            supPolicyFileMapper.selectBatchIds(policyIds).stream()
                    .filter(f -> !nameMap.containsKey(f.getId()))
                    .forEach(f -> nameMap.put(f.getId(), f.getFileName()));
            for (Long id : policyIds) {
                String fileName = nameMap.get(id);
                if (fileName != null) {
                    DetectionFileInfoVO vo = new DetectionFileInfoVO();
                    vo.setFileId(id);
                    vo.setFileName(fileName);
                    vo.setFileType("政策文件");
                    files.add(vo);
                }
            }
        }

        return files;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptIssue(Long recordId, Integer issueIndex) {
        TbDetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResponseCode.DETECTION_NOT_FOUND);
        }
        TbProject project = getProjectOrThrow(record.getProjectId());

        // 提取 original/targeted 用于文档修复
        String original = DetectionResultParser.getIssueField(record.getResult(), issueIndex, "original");
        String targeted = DetectionResultParser.getIssueField(record.getResult(), issueIndex, "targeted");

        int handleStatus = 3; // 3=未找到原文
        if (StringUtils.hasText(original) && StringUtils.hasText(targeted) && !original.equals(targeted)
                && project.getGeneratedFileId() != null) {
            try {
                FixReplacement replacement = new FixReplacement();
                replacement.setOriginal(original);
                replacement.setTargeted(targeted);
                // 传入locationRef供修复引擎精准定位
                LocationRefVO locationRef = DetectionResultParser.parseLocationRef(record.getResult(), issueIndex);
                replacement.setLocationRef(locationRef);
                WordFixResultVO fixResult = fileServiceClient.fixDocument(
                        project.getGeneratedFileId(), List.of(replacement));
                if (fixResult.getFixedCount() > 0) {
                    project.setGeneratedFileId(fixResult.getFileId());
                    projectMapper.updateById(project);
                    handleStatus = 1;
                } else {
                    log.warn("文档修复未找到原文: recordId={}, issueIndex={}", recordId, issueIndex);
                }
            } catch (Exception e) {
                log.error("文档修复失败: recordId={}, issueIndex={}", recordId, issueIndex, e);
            }
        }

        String updatedJson = DetectionResultParser.updateIssueHandleStatus(record.getResult(), issueIndex, handleStatus);
        if (updatedJson != null) {
            record.setResult(updatedJson);
            detectionRecordMapper.updateById(record);
        }

        // 检查是否所有问题已处理，自动转为 DETECTION_PASSED
        tryTransitionToPassed(record.getProjectId());

        log.info("接受检测建议，记录ID: {}, 问题索引: {}, handleStatus: {}", recordId, issueIndex, handleStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectIssue(Long recordId, Integer issueIndex) {
        TbDetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResponseCode.DETECTION_NOT_FOUND);
        }
        getProjectOrThrow(record.getProjectId());

        String updatedJson = DetectionResultParser.updateIssueHandleStatus(
                record.getResult(), issueIndex, 2); // 2=已拒绝
        if (updatedJson != null) {
            record.setResult(updatedJson);
            detectionRecordMapper.updateById(record);
        }

        // 检查是否所有问题已处理，自动转为 DETECTION_PASSED
        tryTransitionToPassed(record.getProjectId());

        log.info("拒绝检测建议，记录ID: {}, 问题索引: {}", recordId, issueIndex);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptAll(Long projectId) {
        TbProject project = getProjectOrThrow(projectId);
        List<TbDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);

        // 先批量标记 handleStatus=1
        for (TbDetectionRecord record : records) {
            String updatedJson = DetectionResultParser.acceptAllIssues(record.getResult());
            if (updatedJson != null) {
                record.setResult(updatedJson);
                detectionRecordMapper.updateById(record);
            }
        }

        // 收集所有已接受的替换项，一次性修复文档
        List<FixReplacement> replacements = DetectionResultParser.parseAcceptedReplacements(records);
        if (!replacements.isEmpty() && project.getGeneratedFileId() != null) {
            try {
                WordFixResultVO fixResult = fileServiceClient.fixDocument(
                        project.getGeneratedFileId(), replacements);
                project.setGeneratedFileId(fixResult.getFileId());
                projectMapper.updateById(project);
                log.info("批量修复文档完成: projectId={}, fixed={}, failed={}",
                        projectId, fixResult.getFixedCount(), fixResult.getFailedCount());
            } catch (Exception e) {
                log.error("批量修复文档失败: projectId={}", projectId, e);
            }
        }

        // 自动转为 DETECTION_PASSED
        tryTransitionToPassed(projectId);

        log.info("批量接受检测建议完成: projectId={}", projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void skip(Long projectId) {
        TbProject project = getProjectOrThrow(projectId);
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
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> retry(Long projectId) {
        TbProject project = getProjectOrThrow(projectId);

        // 修复：走状态机，DETECTION_FAILED → IN_PROGRESS
        ProjectStateMachine.transition(project, ProjectStatus.IN_PROGRESS);
        projectMapper.updateById(project);

        // 重试失败的检测记录
        List<TbDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);
        Map<String, Long> taskIds = new LinkedHashMap<>();
        String detectionContent = buildDetectionContent(project, reviewItemMapper.selectByProjectId(projectId));

        for (TbDetectionRecord record : records) {
            AiTaskStatus status = AiTaskStatus.fromCode(record.getStatus());
            //if (!status.isRetryable()) {
            //    continue;
            //}
            if (DetectionType.POLICY_REVIEW.getCode().equals(record.getDetectionType())
                    && !hasPolicyFileIds(record.getPolicyFileIds())) {
                completeSkippedPolicyReview(record, detectionContent);
                log.info("未选择政策文件，重新检测时跳过政策文件审查: projectId={}, recordId={}",
                        projectId, record.getId());
                continue;
            }

            record.setStatus(AiTaskStatus.PENDING.getCode());
            record.setResult(null);
            record.setContentFileId(null);
            record.setContentSnapshot(detectionContent);
            record.setStartedAt(LocalDateTime.now());
            record.setCompletedAt(null);
            detectionRecordMapper.updateById(record);

            DetectionParams params = new DetectionParams();
            params.setContent(detectionContent);

            AiTaskType taskType = AiTaskType.mapToTaskType(DetectionType.fromCode(record.getDetectionType()));
            AiTask task = aiTaskService.createTask(taskType, projectId,
                    record.getId(), "DETECTION", params, record.getPolicyFileIds());
            record.setTaskId(task.getId());
            detectionRecordMapper.updateById(record);

            taskIds.put(record.getDetectionType(), task.getId());
        }

        // 走状态机回到 DETECTING
        ProjectStateMachine.transition(project, ProjectStatus.PENDING_DETECTION);
        ProjectStateMachine.transition(project, ProjectStatus.DETECTING);
        projectMapper.updateById(project);

        log.info("重新检测，项目ID: {}, 重试任务数: {}", projectId, taskIds.size());
        return taskIds;
    }

    private boolean hasPolicyFileIds(String policyFileIds) {
        if (!StringUtils.hasText(policyFileIds)) {
            return false;
        }
        return Arrays.stream(policyFileIds.split(","))
                .map(String::trim)
                .anyMatch(StringUtils::hasText);
    }

    private void completeSkippedPolicyReview(TbDetectionRecord record, String detectionContent) {
        record.setStatus(AiTaskStatus.COMPLETED.getCode());
        record.setResult(EMPTY_PASS_RESULT);
        record.setContentFileId(null);
        record.setContentSnapshot(detectionContent);
        record.setStartedAt(LocalDateTime.now());
        record.setCompletedAt(LocalDateTime.now());
        detectionRecordMapper.updateById(record);
    }

    private String buildDetectionContent(TbProject project, List<TbProjectReviewItem> reviewItems) {
        StringJoiner content = new StringJoiner("\n\n");
        if (StringUtils.hasText(project.getRequirementContent())) {
            content.add("Requirement Content\n" + project.getRequirementContent());
        }

        StringJoiner reviewItemContent = new StringJoiner("\n");
        if (reviewItems != null) {
            for (TbProjectReviewItem item : reviewItems) {
                String itemStandard = item.getItemStandard();
                if (!StringUtils.hasText(itemStandard)) {
                    continue;
                }
                String prefix = StringUtils.hasText(item.getReviewType()) ? "[" + item.getReviewType() + "] " : "";
                reviewItemContent.add("- " + prefix + itemStandard);
            }
        }

        String reviewItemsText = reviewItemContent.toString();
        if (StringUtils.hasText(reviewItemsText)) {
            content.add("Review Items\n" + reviewItemsText);
        }
        return content.toString();
    }

    private TbProject getProjectOrThrow(Long projectId) {
        TbProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }
        DataScopeHelper.checkOwnership(project.getCreateId());
        return project;
    }

    /**
     * 检查是否所有问题已处理，如果是则自动转为 DETECTION_PASSED
     */
    @Override
    public void tryTransitionToPassed(Long projectId) {
        List<TbDetectionRecord> records = detectionRecordMapper.selectByProjectId(projectId);
        if (DetectionResultParser.hasUnresolvedIssues(records)) {
            return;
        }

        TbProject project = getProjectOrThrow(projectId);
        if (ProjectStatus.DETECTION_FAILED.getCode().equals(project.getStatus())) {
            ProjectStateMachine.transition(project, ProjectStatus.DETECTION_PASSED);
            if (project.getCurrentPhase() < ProjectPhase.DETECTION.getCode()) {
                project.setCurrentPhase(ProjectPhase.DETECTION.getCode());
                project.setProgress(ProjectPhase.DETECTION.getProgressPercent());
                projectMapper.updateById(project);
            }
            projectMapper.updateById(project);
        }

        // 所有问题已处理自动创建版本备份
        try {
            projectVersionService.createVersion(projectId, "所有问题已处理自动备份(" + ProjectStatus.fromCode(project.getStatus()).getLabel() + ")");
            log.info("所有问题已处理版本快照创建成功: projectId={}", projectId);
        } catch (Exception e) {
            log.error("所有问题已处理版本快照创建失败: projectId={}", projectId, e);
        }
    }

    private String getDetectionTypeName(String code) {
        try {
            return DetectionType.fromCode(code).getLabel();
        } catch (Exception e) {
            log.warn("检测类型转换失败: code={}", code, e);
            return code;
        }
    }

    /**
     * 每10秒扫描待处理的检测记录并更新状态
     */
    @Scheduled(fixedDelay = 10000)
    public void updateDetectionPendingStatus() {
        List<TbDetectionRecord> records = detectionRecordMapper.selectList(new QueryWrapper<TbDetectionRecord>().lambda()
                .eq(TbDetectionRecord::getStatus, AiTaskStatus.PENDING.getCode()));
        for (TbDetectionRecord record : records) {
            AiTaskVO task = aiTaskService.getTaskStatus(record.getTaskId());
            record.setStatus(task.getStatus());
            detectionRecordMapper.updateById(record);
        }
    }

}
