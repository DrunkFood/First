package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
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
    private TbProjectReviewItemMapper reviewItemMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private IProjectService projectService;

    @Override
    public List<TbProjectReviewItem> getTreeByProjectId(Long projectId) {
        // 校验项目归属
        projectService.getById(projectId);
        // 返回该项目下所有评审项（扁平列表），前端按reviewType分组显示
        return reviewItemMapper.selectByProjectId(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbProjectReviewItem create(TbProjectReviewItem reviewItem) {
        // 校验项目归属
        projectService.getById(reviewItem.getProjectId());

        // 根据 parentId 自动计算 level（忽略请求中的 level 字段）
        if (reviewItem.getParentId() != null && reviewItem.getParentId() > 0) {
            TbProjectReviewItem parent = reviewItemMapper.selectById(reviewItem.getParentId());
            if (parent == null) {
                throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
            }
            int childLevel = parent.getLevel() + 1;
            if (childLevel > 3) {
                throw new BusinessException(ResponseCode.REVIEW_ITEM_MAX_LEVEL_EXCEEDED);
            }
            reviewItem.setLevel(childLevel);
            // 子项继承父项的 reviewType
            if (reviewItem.getReviewType() == null) {
                reviewItem.setReviewType(parent.getReviewType());
            }
        } else {
            reviewItem.setParentId(null);
            if (reviewItem.getLevel() == null) {
                reviewItem.setLevel(1);
            }
        }

        // 默认排序号
        if (reviewItem.getSortOrder() == null) {
            reviewItem.setSortOrder(0);
        }
        reviewItemMapper.insert(reviewItem);
        return reviewItem;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TbProjectReviewItem reviewItem) {
        TbProjectReviewItem existing = reviewItemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
        }
        // 校验项目归属
        projectService.getById(existing.getProjectId());
        reviewItem.setId(id);
        reviewItemMapper.updateById(reviewItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        TbProjectReviewItem item = reviewItemMapper.selectById(id);
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
        LambdaQueryWrapper<TbProjectReviewItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbProjectReviewItem::getParentId, parentId);
        List<TbProjectReviewItem> children = reviewItemMapper.selectList(wrapper);

        for (TbProjectReviewItem child : children) {
            deleteChildren(child.getId());
            reviewItemMapper.deleteById(child.getId());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiTask submitGenerate(Long projectId, Map<String, Object> params) {
        // 校验项目归属并获取项目信息
        TbProject project = projectService.getById(projectId);
        if (params == null) {
            params = new HashMap<>();
        }

        // 补全项目信息，确保AI生成器有足够的上下文
        params.put("projectId", projectId);
        params.put("projectName", project.getProjectName());
        params.put("projectType", project.getProjectType());
        params.put("projectCategory", project.getProjectCategory());
        params.put("budget", project.getBudget() != null ? project.getBudget().toPlainString() : null);
        params.put("reviewMethod", project.getReviewType());
        params.put("requirementContent", project.getRequirementContent());

        log.info("提交评审项生成: projectId={}, projectName={}", projectId, project.getProjectName());
        return aiTaskService.createTask(AiTaskType.REVIEW_ITEM_GENERATE,
                projectId, projectId, "PROJECT", params, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreate(List<TbProjectReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        // 校验项目归属（取第一个的 projectId）
        Long projectId = items.get(0).getProjectId();
        if (projectId != null) {
            projectService.getById(projectId);
        }
        for (TbProjectReviewItem item : items) {
            if (item.getSortOrder() == null) {
                item.setSortOrder(0);
            }
            reviewItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<TbProjectReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (TbProjectReviewItem item : items) {
            if (item.getId() == null) {
                throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
            }
            TbProjectReviewItem existing = reviewItemMapper.selectById(item.getId());
            if (existing == null) {
                throw new BusinessException(ResponseCode.REVIEW_ITEM_NOT_FOUND);
            }
            // 校验项目归属
            projectService.getById(existing.getProjectId());
            reviewItemMapper.updateById(item);
        }
    }
}
