package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiTemplate;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.TemplateConfigMapper;
import com.jy.eleaitender.support.service.ITemplateConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TemplateConfigServiceImpl implements ITemplateConfigService {

    @Autowired
    private TemplateConfigMapper templateMapper;

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
        wrapper.eq(AiTemplate::getIsDelete, 0);
        wrapper.orderByDesc(AiTemplate::getCreateTime);
        
        return templateMapper.selectPage(page, wrapper);
    }

    @Override
    public AiTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    @Transactional
    public AiTemplate create(AiTemplate template) {
        if (template.getVersionNo() == null) {
            template.setVersionNo(1);
        }
        if (template.getStatus() == null) {
            template.setStatus("ENABLED");
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        templateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public void update(AiTemplate template) {
        templateMapper.updateById(template);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        AiTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResponseCode.TEMPLATE_NOT_FOUND);
        }
        
        // 清除同category+type下的其他默认模板
        UpdateWrapper<AiTemplate> clearWrapper = new UpdateWrapper<>();
        clearWrapper.eq("project_category", template.getProjectCategory());
        clearWrapper.eq("project_type", template.getProjectType());
        clearWrapper.eq("is_delete", 0);
        clearWrapper.set("is_default", 0);
        templateMapper.update(null, clearWrapper);
        
        // 设置当前模板为默认
        AiTemplate update = new AiTemplate();
        update.setId(id);
        update.setIsDefault(1);
        templateMapper.updateById(update);
    }
}
