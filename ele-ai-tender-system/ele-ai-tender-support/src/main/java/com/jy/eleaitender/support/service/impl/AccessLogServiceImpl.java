package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.entity.support.SysAccessLog;
import com.jy.eleaitender.common.mapper.SysAccessLogMapper;
import com.jy.eleaitender.support.service.IAccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 访问日志服务实现
 */
@Service
public class AccessLogServiceImpl extends ServiceImpl<SysAccessLogMapper, SysAccessLog> implements IAccessLogService {

    @Autowired
    private SysAccessLogMapper accessLogMapper;

    @Override
    /**
     * 按条件分页查询访问日志；查询条件全部可选，默认按创建时间倒序。
     */
    public Page<SysAccessLog> getAccessLogPage(Integer pageNum,
                                               Integer pageSize,
                                               String traceId,
                                               String serviceName,
                                               Integer statusCode,
                                               String bizType,
                                               String bizId,
                                               String projectId,
                                               String tenderId,
                                               Date startTime,
                                               Date endTime) {
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
        long size = resolvePageSize(pageSize);

        Page<SysAccessLog> page = new Page<>(current, size);
        LambdaQueryWrapper<SysAccessLog> wrapper = new LambdaQueryWrapper<>();

        // 查询条件尽量按日志索引字段组织，方便后续做 traceId、业务键、状态码检索。
        if (StringUtils.hasText(traceId)) {
            wrapper.eq(SysAccessLog::getTraceId, traceId);
        }
        if (StringUtils.hasText(serviceName)) {
            wrapper.eq(SysAccessLog::getServiceName, serviceName);
        }
        if (statusCode != null) {
            wrapper.eq(SysAccessLog::getStatusCode, statusCode);
        }
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(SysAccessLog::getBizType, bizType);
        }
        if (StringUtils.hasText(bizId)) {
            wrapper.eq(SysAccessLog::getBizId, bizId);
        }
        if (StringUtils.hasText(projectId)) {
            wrapper.eq(SysAccessLog::getProjectId, projectId);
        }
        if (StringUtils.hasText(tenderId)) {
            wrapper.eq(SysAccessLog::getTenderId, tenderId);
        }
        if (startTime != null) {
            wrapper.ge(SysAccessLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysAccessLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(SysAccessLog::getCreateTime);
        return accessLogMapper.selectPage(page, wrapper);
    }

    /**
     * 统一限制页面大小，避免日志查询一次拉出过大结果集。
     */
    private long resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return CommonConstant.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize.intValue(), CommonConstant.MAX_PAGE_SIZE);
    }
}
