package com.jy.eletender.tenderdocument.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExternalSystemAccessMapper extends BaseMapper<SysAccessSystem> {

    @Select("SELECT * FROM sup_access_system WHERE app_key = #{appKey} AND is_delete = 0 LIMIT 1")
    SysAccessSystem selectByAppKey(@Param("appKey") String appKey);
}
