package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务需求Mapper（支撑中心统计用）
 */
@Mapper
public interface AiRequirementMapper extends BaseMapper<AiRequirement> {
}
