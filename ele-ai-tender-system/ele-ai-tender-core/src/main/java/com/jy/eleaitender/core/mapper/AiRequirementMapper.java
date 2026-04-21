package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 业务需求Mapper
 */
@Mapper
public interface AiRequirementMapper extends BaseMapper<AiRequirement> {

}
