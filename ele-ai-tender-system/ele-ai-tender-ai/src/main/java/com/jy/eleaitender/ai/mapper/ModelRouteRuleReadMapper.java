package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SupModelRouteRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型路由规则只读Mapper（AI模块只读访问）
 */
@Mapper
public interface ModelRouteRuleReadMapper extends BaseMapper<SupModelRouteRule> {
}
