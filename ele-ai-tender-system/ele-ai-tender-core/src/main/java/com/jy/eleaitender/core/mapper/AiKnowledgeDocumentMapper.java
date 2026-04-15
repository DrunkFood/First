package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档Mapper（core模块只读查询）
 */
@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {
}
