package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.entity.support.SysUser;
import com.jy.eleaitender.common.entity.support.SysUserRole;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.util.PasswordUtil;
import com.jy.eleaitender.support.mapper.SysUserMapper;
import com.jy.eleaitender.support.mapper.SysUserRoleMapper;
import com.jy.eleaitender.support.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements IUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Override
    public Page<SysUser> getUserPage(Integer pageNum, Integer pageSize, String username, String realName, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (StringUtils.hasText(realName)) {
            wrapper.like(SysUser::getRealName, realName);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        
        wrapper.eq(SysUser::getIsDelete, 0);
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        enrichUserRoles(result.getRecords());
        return result;
    }

    @Override
    public SysUser getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            enrichUserRoles(List.of(user));
        }
        return user;
    }

    @Override
    public SysUser getUserByUsername(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user != null) {
            enrichUserRoles(List.of(user));
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(SysUser user) {
        // 加密密码
        if (user.getPassword() != null) {
            user.setPassword(PasswordUtil.encode(user.getPassword()));
        }
        user.setStatus(1);
        userMapper.insert(user);
        saveUserRoles(user.getId(), user.getRoleIds());
        enrichUserRoles(List.of(user));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        // 不更新密码
        user.setPassword(null);
        userMapper.updateById(user);
        if (user.getRoleIds() != null) {
            saveUserRoles(user.getId(), user.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, id);
        userRoleMapper.delete(wrapper);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.USER_NOT_FOUND);
        }
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResponseCode.USER_PASSWORD_ERROR);
        }
        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(updateUser);
    }

    private void enrichUserRoles(List<SysUser> users) {
        for (SysUser item : users) {
            item.setRoleIds(userMapper.selectRoleIdsByUserId(item.getId()));
            item.setRoles(userMapper.selectRolesByUserId(item.getId()));
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        userRoleMapper.delete(wrapper);

        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }
}
