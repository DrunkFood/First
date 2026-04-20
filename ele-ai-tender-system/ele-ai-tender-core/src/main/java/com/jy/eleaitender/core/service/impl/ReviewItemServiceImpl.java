package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.core.AiReviewItem;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.mapper.AiReviewItemMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IProjectService;
import com.jy.eleaitender.core.service.IReviewItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评审项服务实现
 */
@Slf4j
@Service
public class ReviewItemServiceImpl implements IReviewItemService {

    @Autowired
    private AiReviewItemMapper reviewItemMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private IProjectService projectService;

    @Autowired
    private AiRequirementMapper requirementMapper;

    @Override
    public List<AiReviewItem> getTreeByProjectId(Long projectId) {
        // 校验项目归属
        projectService.getById(projectId);
        // 返回该项目下所有评审项（扁平列表），前端按reviewType分组显示
        return reviewItemMapper.selectByProjectId(projectId);
    }

    @Override
    @Transactional
    public AiReviewItem create(AiReviewItem reviewItem) {
        // 校验项目归属
        projectService.getById(reviewItem.getProjectId());
        // 默认排序号
        if (reviewItem.getSortOrder() == null) {
            reviewItem.setSortOrder(0);
        }
        reviewItemMapper.insert(reviewItem);
        return reviewItem;
    }

    @Override
    @Transactional
    public void update(Long id, AiReviewItem reviewItem) {
        AiReviewItem existing = reviewItemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
        }
        // 校验项目归属
        projectService.getById(existing.getProjectId());
        reviewItem.setId(id);
        reviewItemMapper.updateById(reviewItem);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        AiReviewItem item = reviewItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
        }
        // 校验项目归属
        projectService.getById(item.getProjectId());

        // 级联删除所有子项
        deleteChildren(id);

        // 删除当前项
        reviewItemMapper.deleteById(id);
    }

    /**
     * 递归删除子评审项
     */
    private void deleteChildren(Long parentId) {
        LambdaQueryWrapper<AiReviewItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiReviewItem::getParentId, parentId);
        List<AiReviewItem> children = reviewItemMapper.selectList(wrapper);

        for (AiReviewItem child : children) {
            deleteChildren(child.getId());
            reviewItemMapper.deleteById(child.getId());
        }
    }


    @Override
    @Transactional
    public AiTask submitGenerate(Long projectId, Map<String, Object> params) {
        // 校验项目归属并获取项目信息
        AiProject project = projectService.getById(projectId);
        if (params == null) {
            params = new HashMap<>();
        }

        // 补全项目信息，确保AI生成器有足够的上下文
        params.put("projectId", projectId);
        params.putIfAbsent("projectName", project.getProjectName());
        params.putIfAbsent("projectType", project.getProjectType());
        params.putIfAbsent("projectCategory", project.getProjectCategory());
        params.putIfAbsent("budget", project.getBudget() != null ? project.getBudget().toPlainString() : null);
        params.putIfAbsent("reviewMethod", project.getReviewType());

        // 补全需求内容
        if (!params.containsKey("requirementContent")) {
            AiRequirement requirement = requirementMapper.selectByProjectId(projectId);
            if (requirement != null && requirement.getContent() != null) {
                params.put("requirementContent", requirement.getContent());
            } else if (project.getRequirementContent() != null) {
                params.put("requirementContent", project.getRequirementContent());
            }
        }

        log.info("提交评审项生成: projectId={}, projectName={}", projectId, project.getProjectName());
        return aiTaskService.createTask(AiTaskType.REVIEW_ITEM_GENERATE,
                projectId, projectId, "PROJECT", params, null);
    }

    @Override
    @Transactional
    public void batchCreate(List<AiReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        // 校验项目归属（取第一个的 projectId）
        Long projectId = items.get(0).getProjectId();
        if (projectId != null) {
            projectService.getById(projectId);
        }
        for (AiReviewItem item : items) {
            if (item.getSortOrder() == null) {
                item.setSortOrder(0);
            }
            reviewItemMapper.insert(item);
        }
    }

    @Override
    @Transactional
    public void batchUpdate(List<AiReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (AiReviewItem item : items) {
            if (item.getId() == null) {
                throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
            }
            AiReviewItem existing = reviewItemMapper.selectById(item.getId());
            if (existing == null) {
                throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
            }
            // 校验项目归属
            projectService.getById(existing.getProjectId());
            reviewItemMapper.updateById(item);
        }
    }
}
