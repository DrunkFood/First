package com.jy.eletender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 接入系统Mapper
 */
@Mapper
public interface SysAccessSystemMapper extends BaseMapper<SysAccessSystem> {

    /**
     * 根据AppKey查询接入系统
     */
    @Select("SELECT * FROM sup_access_system WHERE app_key = #{appKey} AND is_delete = 0")
    SysAccessSystem selectByAppKey(@Param("appKey") String appKey);
}
