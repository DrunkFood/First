package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.core.TbProjectTemplate;

/**
 * 项目模板服务接口
 */
public interface IProjectTemplateService extends IService<TbProjectTemplate> {

    /**
     * 项目引用模板 — 从sup_template快照到tb_project_template
     *
     * @param projectId    项目ID
     * @param supTemplateId 源模板ID
     * @return 创建的项目模板快照
     */
    TbProjectTemplate bindTemplate(Long projectId, Long supTemplateId);

    /**
     * 获取项目的模板引用
     *
     * @param projectId 项目ID
     * @return 项目模板快照，不存在返回null
     */
    TbProjectTemplate getByProjectId(Long projectId);

    /**
     * 删除项目的模板引用（项目删除时级联）
     * 物理删除，释放唯一约束
     *
     * @param projectId 项目ID
     */
    void deleteByProjectId(Long projectId);
}
