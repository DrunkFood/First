package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.dto.ai.DetectionParams;
import com.jy.eleaitender.common.dto.ai.RequirementGenerateParams;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import com.jy.eleaitender.common.entity.core.TbRequirement;
import com.jy.eleaitender.common.enums.*;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.MatchFileVO;
import com.jy.eleaitender.core.engine.MarkdownTemplateEngine;
import com.jy.eleaitender.core.engine.WordDocumentGenerator;
import com.jy.eleaitender.core.mapper.AiKnowledgeDocumentMapper;
import com.jy.eleaitender.core.mapper.TbDetectionRecordMapper;
import com.jy.eleaitender.core.mapper.TbRequirementMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IRequirementService;
import com.jy.eleaitender.core.util.DetectionResultParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 业务需求服务实现
 */
@Slf4j
@Service
public class RequirementServiceImpl extends ServiceImpl<TbRequirementMapper, TbRequirement> implements IRequirementService {

    @Autowired
    private TbRequirementMapper requirementMapper;

    @Autowired
    private AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private TbDetectionRecordMapper detectionRecordMapper;

    @Autowired
    private MarkdownTemplateEngine markdownTemplateEngine;

    @Autowired
    private WordDocumentGenerator wordDocumentGenerator;

    private static final DetectionType[] ALL_DETECTION_TYPES = {
            DetectionType.SENSITIVE_WORD,
            DetectionType.TYPO
    };

    @Override
    public Page<TbRequirement> getPage(Integer pageNum, Integer pageSize, String requirementName, String status, String projectType, String createTimeStart, String createTimeEnd) {
        Page<TbRequirement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TbRequirement> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(requirementName)) {
            wrapper.like(TbRequirement::getRequirementName, requirementName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TbRequirement::getStatus, status);
        }
        if (StringUtils.hasText(projectType)) {
            wrapper.eq(TbRequirement::getProjectType, projectType);
        }
        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge(TbRequirement::getCreateTime, createTimeStart + " 00:00:00");
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le(TbRequirement::getCreateTime, createTimeEnd + " 23:59:59");
        }

        wrapper.orderByDesc(TbRequirement::getCreateTime);

        return requirementMapper.selectPage(page, wrapper);
    }

    @Override
    public TbRequirement getById(Long id) {
        TbRequirement requirement = requirementMapper.selectById(id);
        if (requirement == null) {
            throw new BusinessException(ResponseCode.REQUIREMENT_NOT_FOUND);
        }
        DataScopeHelper.checkOwnership(requirement.getCreateId());
        return requirement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbRequirement create(TbRequirement requirement) {
        validateRequirementNameUnique(requirement.getRequirementName(), null);
        if (!StringUtils.hasText(requirement.getStatus())) {
            requirement.setStatus("IN_PROGRESS");
        }
        if (requirement.getProgress() == null) {
            requirement.setProgress(0);
        }
        requirementMapper.insert(requirement);
        return requirement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TbRequirement requirement) {
        getById(id); // 内部已做归属校验
        validateRequirementNameUnique(requirement.getRequirementName(), id);
        requirement.setId(id);
        // 正式保存后清除自动保存内容
        requirement.setAutoSaveContent(null);
        requirement.setAutoSaveTime(null);
        requirementMapper.updateById(requirement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        getById(id); // 内部已做归属校验
        requirementMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbRequirement matchTemplate(Long id, Long matchedFileId, String matchMode) {
        TbRequirement requirement = getById(id);

        requirement.setMatchMode(matchMode);
        requirement.setMatchedFileId(matchedFileId);

        requirementMapper.updateById(requirement);
        return requirement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiTask submitGenerate(Long requirementId, Map<String, Object> extraParams) {
        TbRequirement requirement = getById(requirementId);
        // 清空需求内容
        requirement.setContent("");
        requirement.setProgress(0);
        requirementMapper.updateById(requirement);

        // 构建任务参数
        RequirementGenerateParams taskParams = new RequirementGenerateParams();
        taskParams.setRequirementName(requirement.getRequirementName());
        taskParams.setProjectType(requirement.getProjectType());
        taskParams.setProjectCategory(requirement.getProjectCategory());
        taskParams.setBudget(requirement.getBudget() != null ? requirement.getBudget().toPlainString() : "");
        taskParams.setDescription(requirement.getRequirementDescription());

        // 参考文档内容 从 自动匹配的第一份文件/手动选择匹配的历史文件id/上传的文件id 中获取
        StringJoiner paramJoiner = new StringJoiner(",");
        MatchMode matchMode = MatchMode.fromCode(requirement.getMatchMode());
        switch (matchMode) {
            case AUTO_MATCH:
            case MANUAL_SELECT:
                if (requirement.getMatchedFileId() != null) {
                    paramJoiner.add(String.valueOf(requirement.getMatchedFileId()));
                }
                break;
            case UPLOAD:
                if (requirement.getUploadedFileId() != null) {
                    paramJoiner.add(String.valueOf(requirement.getUploadedFileId()));
                }
                break;
        }
        return aiTaskService.createInternalTask(AiTaskType.REQUIREMENT_GENERATE, null,
                requirementId, BizType.REQUIREMENT.getCode(), taskParams, paramJoiner.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoSave(Long requirementId, String content) {
        TbRequirement requirement = getById(requirementId);
        requirement.setAutoSaveContent(content);
        requirement.setAutoSaveTime(new Date());
        requirementMapper.updateById(requirement);
    }

    @Override
    public String getAutoSaveContent(Long requirementId) {
        TbRequirement requirement = getById(requirementId);
        return requirement.getAutoSaveContent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAutoSave(Long requirementId) {
        TbRequirement requirement = getById(requirementId);
        requirement.setAutoSaveContent(null);
        requirement.setAutoSaveTime(null);
        requirementMapper.updateById(requirement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> submitDetection(Long requirementId) {
        TbRequirement requirement = getById(requirementId);

        // 文档内容快照
        String contentSnapshot = requirement.getContent();

        // 软删除该需求已有的检测记录，避免重新检测时出现重复记录
        // 注意：此操作通过原生SQL绕过@TableLogic，不更新ver/modifyTime等审计字段（软删除后记录不再被业务查询）
        // 注意：进行中的旧AI任务仍会执行完，但syncDetection会因selectById查不到记录而跳过结果同步
        detectionRecordMapper.softDeleteByRequirementId(requirementId);

        // 需求级检测只有2项：敏感词 + 错别字
        Map<String, Long> taskIds = new LinkedHashMap<>();

        // 为每种检测类型创建检测记录 + AI任务
        for (DetectionType type : ALL_DETECTION_TYPES) {
            // 创建检测记录
            TbDetectionRecord record = new TbDetectionRecord();
            record.setRequirementId(requirementId);
            record.setDetectionType(type.getCode());
            record.setContentSnapshot(contentSnapshot);
            record.setStatus(AiTaskStatus.PENDING.getCode());
            record.setStartedAt(new Date());
            detectionRecordMapper.insert(record);

            // 构建AI任务参数
            DetectionParams taskParams = new DetectionParams();
            taskParams.setContent(contentSnapshot);

            AiTaskType taskType = AiTaskType.mapToTaskType(type);

            // bizId=recordId, bizType=DETECTION，使syncDetection能找到record
            AiTask task = aiTaskService.createInternalTask(taskType, null,
                    record.getId(), BizType.DETECTION.getCode(), taskParams, null);

            record.setTaskId(task.getId());
            detectionRecordMapper.updateById(record);

            taskIds.put(type.getCode(), task.getId());
        }

        return taskIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptDetectionIssue(Long requirementId, Long recordId, Integer issueIndex) {
        TbRequirement requirement = getById(requirementId);

        TbDetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null || !requirementId.equals(record.getRequirementId())) {
            throw new BusinessException(ResponseCode.DETECTION_NOT_FOUND);
        }

        // 提取original和targeted用于自动修正
        String original = DetectionResultParser.getIssueField(record.getResult(), issueIndex, "original");
        String targeted = DetectionResultParser.getIssueField(record.getResult(), issueIndex, "targeted");

        int handleStatus = 3; // 3=未找到原文
        // 自动修正：将original替换为targeted（仅替换首次出现，避免多处误替换）
        if (StringUtils.hasText(original) && StringUtils.hasText(targeted) && !original.equals(targeted)) {
            String content = requirement.getContent();
            if (content != null && content.contains(original)) {
                content = content.replaceFirst(Pattern.quote(original), Matcher.quoteReplacement(targeted));
                requirement.setContent(content);
                requirementMapper.updateById(requirement);
                handleStatus = 1;
            } else {
                log.warn("接受建议时原文已不存在，跳过自动修正: requirementId={}, issueIndex={}", requirementId, issueIndex);
            }
        }

        // 更新handleStatus
        String updatedJson = DetectionResultParser.updateIssueHandleStatus(record.getResult(), issueIndex, handleStatus);
        if (updatedJson != null) {
            record.setResult(updatedJson);
            detectionRecordMapper.updateById(record);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectDetectionIssue(Long requirementId, Long recordId, Integer issueIndex) {
        getById(requirementId);

        TbDetectionRecord record = detectionRecordMapper.selectById(recordId);
        if (record == null || !requirementId.equals(record.getRequirementId())) {
            throw new BusinessException(ResponseCode.DETECTION_NOT_FOUND);
        }

        String updatedJson = DetectionResultParser.updateIssueHandleStatus(record.getResult(), issueIndex, 2);
        if (updatedJson != null) {
            record.setResult(updatedJson);
            detectionRecordMapper.updateById(record);
        }
    }

    @Override
    public List<TbDetectionRecord> getDetectionRecords(Long requirementId) {
        getById(requirementId); // 内部已做归属校验
        return detectionRecordMapper.selectByRequirementId(requirementId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishDetection(Long requirementId) {
        TbRequirement requirement = getById(requirementId);
        requirement.setStatus("COMPLETED");
        requirement.setProgress(100);
        requirementMapper.updateById(requirement);
    }

    @Override
    public List<MatchFileVO> getMatchFiles(Long requirementId, String keyword) {
        // 获取当前需求的项目类型信息，用于匹配
        String projectType = null;
        if (requirementId != null) {
            TbRequirement requirement = requirementMapper.selectById(requirementId);
            if (requirement != null) {
                projectType = requirement.getProjectType();
            }
        }

        final String matchProjectType = projectType;
        final String matchKeyword = keyword;

        // 查询知识库文档
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeDocument::getIsDelete, 0);
        wrapper.eq(AiKnowledgeDocument::getStatus, "ACTIVE");

        // 按文档类别（项目类型）筛选
        if (StringUtils.hasText(matchProjectType)) {
            wrapper.eq(AiKnowledgeDocument::getDocCategory, matchProjectType);
        }
        // 按关键词模糊搜索
        if (StringUtils.hasText(matchKeyword)) {
            wrapper.like(AiKnowledgeDocument::getDocName, matchKeyword);
        }
        wrapper.orderByDesc(AiKnowledgeDocument::getCreateTime);
        wrapper.last("LIMIT 20");

        List<AiKnowledgeDocument> documents = knowledgeDocumentMapper.selectList(wrapper);

        // 转换为 MatchFileVO
        return documents.stream().map(doc -> {
            MatchFileVO vo = new MatchFileVO();
            vo.setId(doc.getId());
            vo.setFileName(doc.getDocName());
            vo.setFileType(doc.getFileType());
            vo.setUploadTime(doc.getCreateTime());
            // 匹配度：按项目类型匹配则基础分80，关键词匹配再加分
            int percent = calculateMatchPercent(doc, matchProjectType, matchKeyword);
            vo.setMatchPercent(percent);
            vo.setMatchDesc(buildMatchDesc(doc, matchProjectType, matchKeyword));
            return vo;
        }).collect(Collectors.toList());
    }

    private int calculateMatchPercent(AiKnowledgeDocument doc, String projectType, String keyword) {
        int percent = 50; // 基础匹配分
        if (StringUtils.hasText(projectType) && projectType.equals(doc.getDocCategory())) {
            percent += 30;
        }
        if (StringUtils.hasText(keyword) && StringUtils.hasText(doc.getDocName())
                && doc.getDocName().contains(keyword)) {
            percent += 15;
        }
        return Math.min(percent, 100);
    }

    private String buildMatchDesc(AiKnowledgeDocument doc, String projectType, String keyword) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(projectType) && projectType.equals(doc.getDocCategory())) {
            parts.add("项目类型匹配");
        }
        if (StringUtils.hasText(keyword) && StringUtils.hasText(doc.getDocName())
                && doc.getDocName().contains(keyword)) {
            parts.add("关键词匹配");
        }
        if (parts.isEmpty()) {
            parts.add("同类型文档");
        }
        return String.join("、", parts);
    }

    @Override
    public byte[] exportDocument(Long id) {
        TbRequirement req = getById(id);
        String html = markdownTemplateEngine.markdownToHtml(req.getContent());
        Map<String, Object> data = Map.of("projectName", req.getRequirementName());
        return wordDocumentGenerator.generate(data, html);
    }

    @Override
    public boolean checkNameUnique(String requirementName, Long excludeId) {
        if (!StringUtils.hasText(requirementName)) {
            return true;
        }
        LambdaQueryWrapper<TbRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbRequirement::getRequirementName, requirementName);
        if (excludeId != null) {
            wrapper.ne(TbRequirement::getId, excludeId);
        }
        return requirementMapper.selectCount(wrapper) == 0;
    }

    /**
     * 校验需求名称唯一性（不唯一则抛异常）
     */
    private void validateRequirementNameUnique(String name, Long excludeId) {
        if (!checkNameUnique(name, excludeId)) {
            throw new BusinessException(ResponseCode.REQUIREMENT_NAME_EXISTS, "需求名称已存在: " + name);
        }
    }

}
