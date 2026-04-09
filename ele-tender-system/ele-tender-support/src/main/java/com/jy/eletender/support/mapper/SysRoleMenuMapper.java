package com.jy.eletender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.common.entity.support.SysRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联Mapper
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 物理删除角色菜单关系，避免逻辑删除与唯一索引冲突
     */
    @Delete("DELETE FROM sup_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleIdPhysical(@Param("roleId") Long roleId);
}
