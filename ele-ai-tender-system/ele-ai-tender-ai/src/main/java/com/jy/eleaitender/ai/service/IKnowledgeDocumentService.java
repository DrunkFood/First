package com.jy.eleaitender.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.ai.dto.request.KnowledgeDocumentRequest;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;

public interface IKnowledgeDocumentService {
    Page<AiKnowledgeDocument> getPage(Integer pageNum, Integer pageSize, String docCategory, String status);
    AiKnowledgeDocument getById(Long id);
    AiKnowledgeDocument create(KnowledgeDocumentRequest request);
    void deleteById(Long id);
}
