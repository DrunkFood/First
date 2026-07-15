package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;

public interface IKnowledgeConfigService extends IService<AiKnowledgeDocument> {
    Page<AiKnowledgeDocument> getPage(Integer pageNum, Integer pageSize, String docCategory, String status);
    AiKnowledgeDocument getById(Long id);
    AiKnowledgeDocument create(AiKnowledgeDocument document);
    void update(AiKnowledgeDocument document);
    void deleteById(Long id);
}
