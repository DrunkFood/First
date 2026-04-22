package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.client.InternalFileServiceClient;
import com.jy.eleaitender.common.dto.response.WordStructureVO;
import com.jy.eleaitender.common.entity.support.SupTemplate;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.TemplateConfigMapper;
import com.jy.eleaitender.support.service.ITemplateConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TemplateConfigServiceImpl implements ITemplateConfigService {

    @Autowired
    private TemplateConfigMapper templateMapper;

    @Autowired
    private InternalFileServiceClient fileServiceClient;

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
        wrapper.eq(SupTemplate::getIsDelete, 0);
        wrapper.orderByDesc(SupTemplate::getCreateTime);
        
        return templateMapper.selectPage(page, wrapper);
    }

    @Override
    public SupTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    @Transactional
    public SupTemplate create(SupTemplate template) {
        if (template.getVersionNo() == null) {
            template.setVersionNo(1);
        }
        if (template.getStatus() == null) {
            template.setStatus("ENABLED");
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        // 解析Word文件结构
        if (template.getFileId() != null) {
            try {
                WordStructureVO structure = fileServiceClient.getFileStructure(template.getFileId());
                template.setStructureDefinition(toStructureJson(structure));
            } catch (Exception e) {
                log.warn("解析Word文件结构失败，fileId={}: {}", template.getFileId(), e.getMessage());
            }
        }
        templateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public void update(SupTemplate template) {
        // 如果fileId变更，重新解析Word结构
        if (template.getFileId() != null) {
            SupTemplate existing = templateMapper.selectById(template.getId());
            if (existing == null || !template.getFileId().equals(existing.getFileId())) {
                try {
                    WordStructureVO structure = fileServiceClient.getFileStructure(template.getFileId());
                    template.setStructureDefinition(toStructureJson(structure));
                } catch (Exception e) {
                    log.warn("解析Word文件结构失败，fileId={}: {}", template.getFileId(), e.getMessage());
                }
            }
        }
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
        SupTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResponseCode.TEMPLATE_NOT_FOUND);
        }
        
        // 清除同category+type下的其他默认模板
        UpdateWrapper<SupTemplate> clearWrapper = new UpdateWrapper<>();
        clearWrapper.eq("project_category", template.getProjectCategory());
        clearWrapper.eq("project_type", template.getProjectType());
        clearWrapper.eq("is_delete", 0);
        clearWrapper.set("is_default", 0);
        templateMapper.update(null, clearWrapper);
        
        // 设置当前模板为默认
        SupTemplate update = new SupTemplate();
        update.setId(id);
        update.setIsDefault(1);
        templateMapper.updateById(update);
    }

    private String toStructureJson(WordStructureVO structure) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(structure);
        } catch (Exception e) {
            log.warn("序列化Word结构失败: {}", e.getMessage());
            return null;
        }
    }
}
