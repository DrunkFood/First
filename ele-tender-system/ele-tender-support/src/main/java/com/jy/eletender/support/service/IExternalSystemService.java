package com.jy.eletender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eletender.common.entity.support.SysAccessSystem;

/**
 * 接入系统服务接口
 */
public interface IExternalSystemService {
    
    Page<SysAccessSystem> getSystemPage(Integer pageNum, Integer pageSize, String systemName, String appKey, Integer status);
    
    SysAccessSystem getSystemById(Long id);
    
    SysAccessSystem createSystem(SysAccessSystem system);
    
    void updateSystem(SysAccessSystem system);
    
    void deleteSystem(Long id);
    
    String regenerateSecret(Long id);
    
    void changeStatus(Long id, Integer status);
}
