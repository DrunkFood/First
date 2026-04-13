package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeConfigMapper extends BaseMapper<AiKnowledgeDocument> {
}
