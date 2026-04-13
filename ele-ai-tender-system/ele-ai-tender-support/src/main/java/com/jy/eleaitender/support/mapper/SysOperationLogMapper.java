package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
