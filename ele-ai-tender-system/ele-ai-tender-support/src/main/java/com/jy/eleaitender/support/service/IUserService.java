package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysUser;

/**
 * 用户服务接口
 */
public interface IUserService {
    
    Page<SysUser> getUserPage(Integer pageNum, Integer pageSize, String username, String realName, Integer status);
    
    SysUser getUserById(Long id);
    
    SysUser createUser(SysUser user);
    
    void updateUser(SysUser user);
    
    void deleteUser(Long id);
    
    void resetPassword(Long id, String newPassword);
    
    void changeStatus(Long id, Integer status);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
