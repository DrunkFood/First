package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysOperationLog;
import com.jy.eleaitender.support.mapper.SysOperationLogMapper;
import com.jy.eleaitender.support.service.IOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志服务实现
 */
@Service
public class OperationLogServiceImpl implements IOperationLogService {

    @Autowired
    private SysOperationLogMapper operationLogMapper;

    @Override
    public Page<SysOperationLog> getPage(Integer pageNum, Integer pageSize, String userName, String operation) {
        Page<SysOperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(userName)) {
            wrapper.like(SysOperationLog::getUserName, userName);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.like(SysOperationLog::getOperation, operation);
        }
        wrapper.orderByDesc(SysOperationLog::getCreateTime);

        return operationLogMapper.selectPage(page, wrapper);
    }
}
