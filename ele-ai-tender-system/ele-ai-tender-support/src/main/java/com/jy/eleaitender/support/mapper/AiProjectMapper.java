package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.AiProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI编制项目Mapper（支撑中心统计用）
 */
@Mapper
public interface AiProjectMapper extends BaseMapper<AiProject> {
}
