package com.jy.eletender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.common.entity.support.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统菜单Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID查询菜单列表
     */
    @Select("SELECT DISTINCT m.* FROM sup_menu m " +
            "INNER JOIN sup_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sup_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.is_delete = 0 AND rm.is_delete = 0 AND ur.is_delete = 0 " +
            "AND m.status = 1 ORDER BY m.sort_order")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询菜单ID列表
     */
    @Select("SELECT menu_id FROM sup_role_menu WHERE role_id = #{roleId} AND is_delete = 0")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
