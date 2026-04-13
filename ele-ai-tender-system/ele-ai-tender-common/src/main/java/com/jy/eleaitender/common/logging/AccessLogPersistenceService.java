package com.jy.eleaitender.common.logging;

import com.jy.eleaitender.common.entity.support.SysAccessLog;

/**
 * 访问日志持久化服务。
 */
public interface AccessLogPersistenceService {

    void persist(SysAccessLog accessLog);
}
