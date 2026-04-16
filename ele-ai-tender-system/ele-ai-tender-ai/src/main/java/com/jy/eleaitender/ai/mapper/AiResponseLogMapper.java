package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiResponseLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI响应记录 Mapper
 */
@Mapper
public interface AiResponseLogMapper extends BaseMapper<AiResponseLog> {
}
