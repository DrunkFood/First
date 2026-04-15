package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.AiTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TemplateConfigMapper extends BaseMapper<AiTemplate> {
}
