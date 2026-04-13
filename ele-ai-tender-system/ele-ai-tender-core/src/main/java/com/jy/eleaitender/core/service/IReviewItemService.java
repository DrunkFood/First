package com.jy.eleaitender.core.service;

import com.jy.eleaitender.core.entity.AiReviewItem;

import java.util.List;

/**
 * 评审项服务接口
 */
public interface IReviewItemService {

    /**
     * 根据项目ID获取评审项树
     */
    List<AiReviewItem> getTreeByProjectId(Long projectId);

    /**
     * 创建评审项
     */
    AiReviewItem create(AiReviewItem reviewItem);

    /**
     * 更新评审项
     */
    void update(Long id, AiReviewItem reviewItem);

    /**
     * 删除评审项（级联删除子项）
     */
    void deleteById(Long id);
}
