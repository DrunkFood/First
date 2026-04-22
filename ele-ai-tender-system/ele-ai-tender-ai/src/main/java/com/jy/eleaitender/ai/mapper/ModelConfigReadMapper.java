package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SupModelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型配置只读Mapper（AI模块只读访问）
 */
@Mapper
public interface ModelConfigReadMapper extends BaseMapper<SupModelConfig> {
}
