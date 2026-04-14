package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysOperationLog;

/**
 * 操作日志服务接口
 */
public interface IOperationLogService {

    /**
     * 分页查询操作日志
     *
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param userName  操作用户名（模糊查询）
     * @param operation 操作类型（模糊查询）
     * @return 分页结果
     */
    Page<SysOperationLog> getPage(Integer pageNum, Integer pageSize, String userName, String operation);
}
