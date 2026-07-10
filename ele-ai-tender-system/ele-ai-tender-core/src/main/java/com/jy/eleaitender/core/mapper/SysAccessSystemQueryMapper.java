package com.jy.eleaitender.core.mapper;

import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 接入系统查询Mapper（core模块只读，不依赖support模块）
 */
@Mapper
public interface SysAccessSystemQueryMapper {

    /**
     * 根据AppKey查询接入系统
     */
    @Select("SELECT * FROM sup_access_system WHERE app_key = #{appKey} AND is_delete = 0")
    SysAccessSystem selectByAppKey(@Param("appKey") String appKey);
}
