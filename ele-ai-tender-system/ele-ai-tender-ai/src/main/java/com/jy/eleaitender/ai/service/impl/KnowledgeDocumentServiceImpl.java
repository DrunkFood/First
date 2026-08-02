package com.jy.eleaitender.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.ai.dto.request.KnowledgeDocumentRequest;
import com.jy.eleaitender.ai.mapper.AiKnowledgeDocumentMapper;
import com.jy.eleaitender.ai.service.IKnowledgeDocumentService;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<AiKnowledgeDocumentMapper, AiKnowledgeDocument> implements IKnowledgeDocumentService {

    @Autowired
    private AiKnowledgeDocumentMapper knowledgeDocumentMapper;

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
    public AiKnowledgeDocument create(KnowledgeDocumentRequest request) {
        AiKnowledgeDocument doc = new AiKnowledgeDocument();
        doc.setDocName(request.getDocName());
        doc.setDocCategory(request.getDocCategory());
        doc.setFileId(request.getFileId());
        doc.setFileType(request.getFileType());
        doc.setStatus("ACTIVE");
        knowledgeDocumentMapper.insert(doc);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        knowledgeDocumentMapper.deleteById(id);
    }
}
