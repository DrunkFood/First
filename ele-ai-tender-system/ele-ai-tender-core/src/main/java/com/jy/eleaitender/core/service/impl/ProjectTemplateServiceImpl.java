package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectTemplate;
import com.jy.eleaitender.common.entity.support.SupTemplate;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.mapper.ProjectTemplateMapper;
import com.jy.eleaitender.core.mapper.SupTemplateMapper;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.service.IProjectTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目模板服务实现
 */
@Slf4j
@Service
public class ProjectTemplateServiceImpl extends ServiceImpl<ProjectTemplateMapper, TbProjectTemplate> implements IProjectTemplateService {

    @Autowired
    private ProjectTemplateMapper projectTemplateMapper;

    @Autowired
    private SupTemplateMapper supTemplateMapper;

    @Autowired
    private TbProjectMapper projectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbProjectTemplate bindTemplate(Long projectId, Long supTemplateId) {
        // 校验源模板存在
        SupTemplate supTemplate = supTemplateMapper.selectById(supTemplateId);
        if (supTemplate == null) {
            throw new BusinessException(ResponseCode.TEMPLATE_NOT_FOUND);
        }
        // 校验项目存在
        TbProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }

        // 删除旧绑定（物理删除，释放唯一约束）
        deleteByProjectId(projectId);

        // 从源模板创建快照
        TbProjectTemplate pt = new TbProjectTemplate();
        pt.setProjectId(projectId);
        pt.setTemplateId(supTemplate.getId());
        pt.setTemplateName(supTemplate.getTemplateName());
        pt.setProjectCategory(supTemplate.getProjectCategory());
        pt.setProjectType(supTemplate.getProjectType());
        pt.setFileId(supTemplate.getFileId());
        pt.setContent(supTemplate.getContent());
        pt.setStructureDefinition(supTemplate.getStructureDefinition());
        pt.setReviewConfig(supTemplate.getReviewConfig());         // 评审项配置JSON(快照)
        pt.setVersionNo(supTemplate.getVersionNo());
        projectTemplateMapper.insert(pt);

        // 更新项目的templateId指向新快照
        project.setTemplateId(pt.getId());
        projectMapper.updateById(project);

        log.info("项目绑定模板: projectId={}, supTemplateId={}, snapshotId={}", projectId, supTemplateId, pt.getId());
        return pt;
    }

    @Override
    public TbProjectTemplate getByProjectId(Long projectId) {
        LambdaQueryWrapper<TbProjectTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbProjectTemplate::getProjectId, projectId);
        return projectTemplateMapper.selectOne(wrapper);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        // 物理删除：BaseEntity有@TableLogic，MyBatis-Plus的delete会变成逻辑删除，
        // 无法释放唯一约束，因此使用自定义的physicalDeleteByProjectId绕过逻辑删除
        projectTemplateMapper.physicalDeleteByProjectId(projectId);
        log.info("物理删除项目模板引用: projectId={}", projectId);
    }
}
