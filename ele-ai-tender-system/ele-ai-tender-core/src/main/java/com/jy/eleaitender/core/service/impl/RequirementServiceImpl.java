package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.MatchFileVO;
import com.jy.eleaitender.core.mapper.AiKnowledgeDocumentMapper;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IRequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 业务需求服务实现
 */
@Service
public class RequirementServiceImpl implements IRequirementService {

    @Autowired
    private AiRequirementMapper requirementMapper;

    @Autowired
    private AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Override
    public Page<AiRequirement> getPage(Integer pageNum, Integer pageSize, String requirementName, String status, Long projectId) {
        Page<AiRequirement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiRequirement> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(requirementName)) {
            wrapper.like(AiRequirement::getRequirementName, requirementName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AiRequirement::getStatus, status);
        }
        if (projectId != null) {
            wrapper.eq(AiRequirement::getProjectId, projectId);
        }

        wrapper.orderByDesc(AiRequirement::getCreateTime);

        return requirementMapper.selectPage(page, wrapper);
    }

    @Override
    public AiRequirement getById(Long id) {
        AiRequirement requirement = requirementMapper.selectById(id);
        if (requirement == null) {
            throw new BusinessException(ResponseCode.REQUIREMENT_NOT_FOUND);
        }
        DataScopeHelper.checkOwnership(requirement.getCreateId());
        return requirement;
    }

    @Override
    @Transactional
    public AiRequirement create(AiRequirement requirement) {
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
    @Transactional
    public void update(Long id, AiRequirement requirement) {
        getById(id); // 内部已做归属校验
        requirement.setId(id);
        // 正式保存后清除自动保存内容
        requirement.setAutoSaveContent(null);
        requirement.setAutoSaveTime(null);
        requirementMapper.updateById(requirement);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        AiRequirement requirement = getById(id); // 内部已做归属校验
        requirementMapper.deleteById(id);
    }

    @Override
    @Transactional
    public AiRequirement matchTemplate(Long id, Long matchedFileId, String matchMode) {
        AiRequirement requirement = getById(id);

        requirement.setMatchMode(matchMode);
        requirement.setMatchedFileId(matchedFileId);

        requirementMapper.updateById(requirement);
        return requirement;
    }

    @Override
    @Transactional
    public AiTask submitGenerate(Long requirementId, Map<String, Object> params) {
        AiRequirement requirement = getById(requirementId);
        // 清空需求内容
        requirement.setContent("");
        requirement.setProgress(0);
        requirementMapper.updateById(requirement);

        // 填充任务参数
        params.put("requirementId", requirementId);
        params.put("requirementName", requirement.getRequirementName());
        params.put("projectType", requirement.getProjectType());
        params.put("projectCategory", requirement.getProjectCategory());
        params.put("budget", requirement.getBudget());
        params.put("description", requirement.getRequirementDescription());
        // TODO 参考文档内容 从 自动匹配的第一份文件/手动选择匹配的历史文件id/上传的文件id 中获取
        params.put("referenceContent", "");
        return aiTaskService.createTask(AiTaskType.REQUIREMENT_GENERATE,
                requirement.getProjectId(), requirementId, "REQUIREMENT", params, null);
    }

    @Override
    @Transactional
    public void autoSave(Long requirementId, String content) {
        AiRequirement requirement = getById(requirementId);
        requirement.setAutoSaveContent(content);
        requirement.setAutoSaveTime(new Date());
        requirementMapper.updateById(requirement);
    }

    @Override
    public String getAutoSaveContent(Long requirementId) {
        AiRequirement requirement = getById(requirementId);
        return requirement.getAutoSaveContent();
    }

    @Override
    @Transactional
    public void clearAutoSave(Long requirementId) {
        AiRequirement requirement = getById(requirementId);
        requirement.setAutoSaveContent(null);
        requirement.setAutoSaveTime(null);
        requirementMapper.updateById(requirement);
    }

    @Override
    @Transactional
    public Map<String, Long> submitDetection(Long requirementId) {
        AiRequirement requirement = getById(requirementId);
        Map<String, Object> params = new HashMap<>();
        params.put("requirementId", requirementId);
        params.put("content", requirement.getContent());

        Map<String, Long> taskIds = new HashMap<>();
        AiTask sensitiveTask = aiTaskService.createTask(AiTaskType.DETECTION_SENSITIVE_WORD,
                requirement.getProjectId(), requirementId, "REQUIREMENT", params, null);
        taskIds.put("SENSITIVE_WORD", sensitiveTask.getId());

        AiTask typoTask = aiTaskService.createTask(AiTaskType.DETECTION_TYPO,
                requirement.getProjectId(), requirementId, "REQUIREMENT", params, null);
        taskIds.put("TYPO", typoTask.getId());

        return taskIds;
    }

    @Override
    public List<MatchFileVO> getMatchFiles(Long requirementId, String keyword) {
        // 获取当前需求的项目类型信息，用于匹配
        String projectType = null;
        if (requirementId != null) {
            AiRequirement requirement = requirementMapper.selectById(requirementId);
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
}
