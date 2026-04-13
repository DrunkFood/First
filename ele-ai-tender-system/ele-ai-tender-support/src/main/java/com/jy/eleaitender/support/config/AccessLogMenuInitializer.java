package com.jy.eleaitender.support.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.support.SysMenu;
import com.jy.eleaitender.common.entity.support.SysRole;
import com.jy.eleaitender.common.entity.support.SysRoleMenu;
import com.jy.eleaitender.support.mapper.SysMenuMapper;
import com.jy.eleaitender.support.mapper.SysRoleMapper;
import com.jy.eleaitender.support.mapper.SysRoleMenuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 访问日志菜单初始化。
 */
@Slf4j
@Component
public class AccessLogMenuInitializer implements ApplicationRunner {

    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String SYSTEM_MENU_CODE = "system";
    private static final String ACCESS_LOG_MENU_CODE = "access-log";
    private static final String ACCESS_LOG_PERMISSION = "access-log:view";

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public AccessLogMenuInitializer(SysMenuMapper menuMapper,
                                    SysRoleMapper roleMapper,
                                    SysRoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        SysMenu parentMenu = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuCode, SYSTEM_MENU_CODE)
                .last("LIMIT 1"));
        if (parentMenu == null) {
            log.warn("未找到系统管理菜单，跳过访问日志菜单初始化");
            return;
        }

        SysMenu accessLogMenu = menuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuCode, ACCESS_LOG_MENU_CODE)
                .last("LIMIT 1"));
        if (accessLogMenu == null) {
            accessLogMenu = new SysMenu();
            accessLogMenu.setParentId(parentMenu.getId());
            accessLogMenu.setMenuName("访问日志");
            accessLogMenu.setMenuCode(ACCESS_LOG_MENU_CODE);
            accessLogMenu.setMenuType(1);
            accessLogMenu.setMenuUrl("/system/access-log");
            accessLogMenu.setPermission(ACCESS_LOG_PERMISSION);
            accessLogMenu.setSortOrder(5);
            accessLogMenu.setStatus(1);
            menuMapper.insert(accessLogMenu);
            log.info("已初始化访问日志菜单 menuId={}", accessLogMenu.getId());
        }

        SysRole adminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, ADMIN_ROLE_CODE)
                .last("LIMIT 1"));
        if (adminRole == null) {
            log.warn("未找到管理员角色，跳过访问日志菜单授权");
            return;
        }

        Long count = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, adminRole.getId())
                .eq(SysRoleMenu::getMenuId, accessLogMenu.getId()));
        if (count != null && count.longValue() > 0L) {
            return;
        }

        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(adminRole.getId());
        roleMenu.setMenuId(accessLogMenu.getId());
        roleMenuMapper.insert(roleMenu);
        log.info("已为管理员角色授权访问日志菜单 roleId={} menuId={}", adminRole.getId(), accessLogMenu.getId());
    }
}
