package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.support.SupTemplate;
import com.jy.eleaitender.core.mapper.SupTemplateMapper;
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
    private SupTemplateMapper templateMapper;

    @Override
    public Page<SupTemplate> getPage(Integer pageNum, Integer pageSize, String templateName, String projectCategory, String projectType) {
        Page<SupTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SupTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(templateName)) {
            wrapper.like(SupTemplate::getTemplateName, templateName);
        }
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(SupTemplate::getProjectCategory, projectCategory);
        }
        if (StringUtils.hasText(projectType)) {
            wrapper.eq(SupTemplate::getProjectType, projectType);
        }

        wrapper.orderByDesc(SupTemplate::getCreateTime);

        return templateMapper.selectPage(page, wrapper);
    }

    @Override
    public SupTemplate getById(Long id) {
        SupTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResponseCode.TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    @Override
    public SupTemplate getDefault(String projectCategory, String projectType) {
        LambdaQueryWrapper<SupTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupTemplate::getIsDefault, 1);
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(SupTemplate::getProjectCategory, projectCategory);
        }
        if (StringUtils.hasText(projectType)) {
            wrapper.eq(SupTemplate::getProjectType, projectType);
        }
        wrapper.last("LIMIT 1");

        return templateMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupTemplate create(SupTemplate template) {
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
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SupTemplate template) {
        SupTemplate existing = getById(id);
        template.setId(id);
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的模板");
        }
        for (Long id : ids) {
            templateMapper.deleteById(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        SupTemplate template = getById(id);

        // 先清除同项目类别和类型的其他默认模板
        LambdaQueryWrapper<SupTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupTemplate::getIsDefault, 1);
        wrapper.eq(SupTemplate::getProjectCategory, template.getProjectCategory());
        wrapper.eq(SupTemplate::getProjectType, template.getProjectType());

        List<SupTemplate> oldDefaults = templateMapper.selectList(wrapper);
        for (SupTemplate oldDefault : oldDefaults) {
            SupTemplate update = new SupTemplate();
            update.setId(oldDefault.getId());
            update.setIsDefault(0);
            templateMapper.updateById(update);
        }

        // 设置当前模板为默认
        SupTemplate update = new SupTemplate();
        update.setId(id);
        update.setIsDefault(1);
        templateMapper.updateById(update);
    }
}
