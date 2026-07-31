package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SysUser;
import com.jy.eleaitender.common.entity.support.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统用户Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sup_user WHERE username = #{username} AND is_delete = 0")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据用户名查询用户（包含逻辑删除数据，用于处理手机号自动注册唯一索引冲突）
     */
    @Select("SELECT * FROM sup_user WHERE username = #{username} LIMIT 1")
    SysUser selectAnyByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM sup_user WHERE phone = #{phone} AND is_delete = 0")
    SysUser selectByPhone(@Param("phone") String phone);

    /**
     * 重新启用已逻辑删除的手机号账号
     */
    @Update("UPDATE sup_user SET phone = #{phone}, status = 1, is_delete = 0, modify_time = NOW(), modify_id = 0, modify_name = 'system' WHERE username = #{phone}")
    int reactivateByUsername(@Param("phone") String phone);

    /**
     * 根据用户ID查询角色编码列表
     */
    @Select("SELECT r.role_code FROM sup_role r " +
            "INNER JOIN sup_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.is_delete = 0 AND ur.is_delete = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询权限标识列表
     */
    @Select("SELECT DISTINCT m.permission FROM sup_menu m " +
            "INNER JOIN sup_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sup_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.is_delete = 0 AND rm.is_delete = 0 AND ur.is_delete = 0 " +
            "AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.id FROM sup_role r " +
            "INNER JOIN sup_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.is_delete = 0 AND ur.is_delete = 0")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.* FROM sup_role r " +
            "INNER JOIN sup_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.is_delete = 0 AND ur.is_delete = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
