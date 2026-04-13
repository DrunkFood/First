package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.core.entity.AiRequirement;

/**
 * 业务需求服务接口
 */
public interface IRequirementService {

    /**
     * 分页查询需求列表
     */
    Page<AiRequirement> getPage(Integer pageNum, Integer pageSize, String requirementName, String status, Long projectId);

    /**
     * 根据ID获取需求详情
     */
    AiRequirement getById(Long id);

    /**
     * 创建需求
     */
    AiRequirement create(AiRequirement requirement);

    /**
     * 更新需求
     */
    void update(Long id, AiRequirement requirement);

    /**
     * 删除需求
     */
    void deleteById(Long id);

    /**
     * 匹配历史模板
     */
    AiRequirement matchTemplate(Long id, Long matchedFileId, String matchMode);
}
