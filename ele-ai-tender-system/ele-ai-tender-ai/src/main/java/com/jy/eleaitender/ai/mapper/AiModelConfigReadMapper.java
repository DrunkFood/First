package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型配置只读Mapper（AI模块只读访问）
 */
@Mapper
public interface AiModelConfigReadMapper extends BaseMapper<AiModelConfig> {
}
