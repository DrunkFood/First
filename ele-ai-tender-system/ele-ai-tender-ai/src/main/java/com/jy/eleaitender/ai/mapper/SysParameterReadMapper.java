package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SysParameter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统参数只读Mapper（AI模块只读访问）
 */
@Mapper
public interface SysParameterReadMapper extends BaseMapper<SysParameter> {
}
