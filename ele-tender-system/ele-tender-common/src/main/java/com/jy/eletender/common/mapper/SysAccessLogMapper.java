package com.jy.eletender.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.common.entity.support.SysAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统访问日志Mapper
 */
@Mapper
public interface SysAccessLogMapper extends BaseMapper<SysAccessLog> {
}
