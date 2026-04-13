package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelConfigMapper extends BaseMapper<AiModelConfig> {
}
