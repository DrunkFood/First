package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;

import java.util.List;
import java.util.Map;

/**
 * 评审项服务接口
 */
public interface IReviewItemService extends IService<TbProjectReviewItem> {

    /**
     * 根据项目ID获取评审项树
     */
    List<TbProjectReviewItem> getTreeByProjectId(Long projectId);

    /**
     * 创建评审项
     */
    TbProjectReviewItem create(TbProjectReviewItem reviewItem);

    /**
     * 更新评审项
     */
    void update(Long id, TbProjectReviewItem reviewItem);

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
    void batchCreate(List<TbProjectReviewItem> items);

    /**
     * 批量更新评审项
     */
    void batchUpdate(List<TbProjectReviewItem> items);

    /**
     * 替换项目下全部评审项
     */
    void replaceAll(Long projectId, List<TbProjectReviewItem> items);
}
