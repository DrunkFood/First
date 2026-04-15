package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.core.AiReviewItem;
import com.jy.eleaitender.core.mapper.AiReviewItemMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IReviewItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评审项服务实现
 */
@Service
public class ReviewItemServiceImpl implements IReviewItemService {

    @Autowired
    private AiReviewItemMapper reviewItemMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Override
    public List<AiReviewItem> getTreeByProjectId(Long projectId) {
        // 加载该项目下所有评审项
        List<AiReviewItem> allItems = reviewItemMapper.selectByProjectId(projectId);
        if (allItems.isEmpty()) {
            return new ArrayList<>();
        }

        // 在内存中构建树形结构
        return buildTree(allItems);
    }

    @Override
    @Transactional
    public AiReviewItem create(AiReviewItem reviewItem) {
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

    /**
     * 在内存中构建树形结构
     */
    private List<AiReviewItem> buildTree(List<AiReviewItem> allItems) {
        // 找出根节点（parentId 为 null）
        List<AiReviewItem> rootItems = new ArrayList<>();

        for (AiReviewItem item : allItems) {
            if (item.getParentId() == null) {
                rootItems.add(item);
            }
        }

        // 对每个根节点递归构建子树
        for (AiReviewItem root : rootItems) {
            buildChildren(root, allItems);
        }

        return rootItems;
    }

    /**
     * 递归构建子节点
     */
    private void buildChildren(AiReviewItem parent, List<AiReviewItem> allItems) {
        List<AiReviewItem> children = allItems.stream()
                .filter(item -> parent.getId().equals(item.getParentId()))
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            // 注意：由于 AiReviewItem 实体没有 children 字段，这里仅返回扁平列表
            // 实际前端树形展示依赖 parentId 关系在前端构建树
            for (AiReviewItem child : children) {
                buildChildren(child, allItems);
            }
        }
    }

    @Override
    @Transactional
    public AiTask submitGenerate(Long projectId, Map<String, Object> params) {
        params.put("projectId", projectId);
        return aiTaskService.createTask(AiTaskType.REVIEW_ITEM_GENERATE,
                projectId, projectId, "PROJECT", params, null);
    }

    @Override
    @Transactional
    public void batchCreate(List<AiReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return;
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
            reviewItemMapper.updateById(item);
        }
    }
}
