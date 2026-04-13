package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.entity.AiRequirement;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.service.IRequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 业务需求服务实现
 */
@Service
public class RequirementServiceImpl implements IRequirementService {

    @Autowired
    private AiRequirementMapper requirementMapper;

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
        // 初始化状态为草稿
        if (!StringUtils.hasText(requirement.getStatus())) {
            requirement.setStatus("DRAFT");
        }
        requirementMapper.insert(requirement);
        return requirement;
    }

    @Override
    @Transactional
    public void update(Long id, AiRequirement requirement) {
        AiRequirement existing = getById(id);
        requirement.setId(id);
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
}
