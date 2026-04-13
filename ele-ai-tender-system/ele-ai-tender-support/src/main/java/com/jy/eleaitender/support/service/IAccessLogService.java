package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysAccessLog;

import java.util.Date;

/**
 * 访问日志服务
 */
public interface IAccessLogService {

    Page<SysAccessLog> getAccessLogPage(Integer pageNum,
                                        Integer pageSize,
                                        String traceId,
                                        String serviceName,
                                        Integer statusCode,
                                        String bizType,
                                        String bizId,
                                        String projectId,
                                        String tenderId,
                                        Date startTime,
                                        Date endTime);
}
