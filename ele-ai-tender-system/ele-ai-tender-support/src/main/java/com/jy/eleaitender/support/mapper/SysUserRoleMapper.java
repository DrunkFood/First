package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("SELECT COUNT(1) FROM sup_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    Long countAnyByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Update("UPDATE sup_user_role SET is_delete = 0, modify_time = NOW(), modify_id = 0, modify_name = 'system' WHERE user_id = #{userId} AND role_id = #{roleId}")
    int reactivateByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
