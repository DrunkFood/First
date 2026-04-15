package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IRequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 业务需求服务实现
 */
@Service
public class RequirementServiceImpl implements IRequirementService {

    @Autowired
    private AiRequirementMapper requirementMapper;

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
        getById(id);
        requirement.setId(id);
        // 正式保存后清除自动保存内容
        requirement.setAutoSaveContent(null);
        requirement.setAutoSaveTime(null);
        requirementMapper.updateById(requirement);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        AiRequirement requirement = getById(id);
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
        params.put("requirementId", requirementId);
        params.put("requirementName", requirement.getRequirementName());
        params.put("projectType", requirement.getProjectType());
        params.put("budget", requirement.getBudget());
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
}
