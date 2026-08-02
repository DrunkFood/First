package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import com.jy.eleaitender.support.mapper.KnowledgeConfigMapper;
import com.jy.eleaitender.support.service.IKnowledgeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeConfigServiceImpl extends ServiceImpl<KnowledgeConfigMapper, AiKnowledgeDocument> implements IKnowledgeConfigService {

    @Autowired
    private KnowledgeConfigMapper knowledgeDocumentMapper;

    @Override
    public Page<AiKnowledgeDocument> getPage(Integer pageNum, Integer pageSize, String docCategory, String status) {
        Page<AiKnowledgeDocument> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(docCategory)) {
            wrapper.eq(AiKnowledgeDocument::getDocCategory, docCategory);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AiKnowledgeDocument::getStatus, status);
        }
        wrapper.eq(AiKnowledgeDocument::getIsDelete, 0);
        wrapper.orderByDesc(AiKnowledgeDocument::getCreateTime);
        
        return knowledgeDocumentMapper.selectPage(page, wrapper);
    }

    @Override
    public AiKnowledgeDocument getById(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocument create(AiKnowledgeDocument document) {
        if (document.getStatus() == null) {
            document.setStatus("ACTIVE");
        }
        knowledgeDocumentMapper.insert(document);
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AiKnowledgeDocument document) {
        knowledgeDocumentMapper.updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        knowledgeDocumentMapper.deleteById(id);
    }
}
