package com.jy.eleaitender.core.service;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiReviewItem;

import java.util.List;
import java.util.Map;

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

    /**
     * 提交AI生成评审项任务
     */
    AiTask submitGenerate(Long projectId, Map<String, Object> params);

    /**
     * 批量创建评审项
     */
    void batchCreate(List<AiReviewItem> items);

    /**
     * 批量更新评审项
     */
    void batchUpdate(List<AiReviewItem> items);
}
