package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.core.AiTemplate;
import com.jy.eleaitender.core.mapper.AiTemplateMapper;
import com.jy.eleaitender.core.service.ITemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 模板服务实现
 */
@Service
public class TemplateServiceImpl implements ITemplateService {

    @Autowired
    private AiTemplateMapper templateMapper;

    @Override
    public Page<AiTemplate> getPage(Integer pageNum, Integer pageSize, String templateName, String projectCategory, String projectType) {
        Page<AiTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(templateName)) {
            wrapper.like(AiTemplate::getTemplateName, templateName);
        }
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(AiTemplate::getProjectCategory, projectCategory);
        }
        if (StringUtils.hasText(projectType)) {
            wrapper.eq(AiTemplate::getProjectType, projectType);
        }

        wrapper.orderByDesc(AiTemplate::getCreateTime);

        return templateMapper.selectPage(page, wrapper);
    }

    @Override
    public AiTemplate getById(Long id) {
        AiTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResponseCode.TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    @Override
    public AiTemplate getDefault(String projectCategory, String projectType) {
        LambdaQueryWrapper<AiTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiTemplate::getIsDefault, 1);
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(AiTemplate::getProjectCategory, projectCategory);
        }
        if (StringUtils.hasText(projectType)) {
            wrapper.eq(AiTemplate::getProjectType, projectType);
        }
        wrapper.last("LIMIT 1");

        return templateMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public AiTemplate create(AiTemplate template) {
        // 新模板默认非默认
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        // 默认版本号
        if (template.getVersionNo() == null) {
            template.setVersionNo(1);
        }
        if (!StringUtils.hasText(template.getStatus())) {
            template.setStatus("ENABLED");
        }
        templateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public void update(Long id, AiTemplate template) {
        AiTemplate existing = getById(id);
        template.setId(id);
        templateMapper.updateById(template);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的模板");
        }
        for (Long id : ids) {
            templateMapper.deleteById(id);
        }
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        AiTemplate template = getById(id);

        // 先清除同项目类别和类型的其他默认模板
        LambdaQueryWrapper<AiTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiTemplate::getIsDefault, 1);
        wrapper.eq(AiTemplate::getProjectCategory, template.getProjectCategory());
        wrapper.eq(AiTemplate::getProjectType, template.getProjectType());

        List<AiTemplate> oldDefaults = templateMapper.selectList(wrapper);
        for (AiTemplate oldDefault : oldDefaults) {
            AiTemplate update = new AiTemplate();
            update.setId(oldDefault.getId());
            update.setIsDefault(0);
            templateMapper.updateById(update);
        }

        // 设置当前模板为默认
        AiTemplate update = new AiTemplate();
        update.setId(id);
        update.setIsDefault(1);
        templateMapper.updateById(update);
    }
}
