package com.jy.eleaitender.common.logging;

import com.jy.eleaitender.common.entity.support.SysAccessLog;
import com.jy.eleaitender.common.mapper.SysAccessLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;

import java.util.Date;

/**
 * 基于数据库的访问日志持久化服务。
 */
@Slf4j
public class DbAccessLogPersistenceService implements AccessLogPersistenceService {

    private final SysAccessLogMapper accessLogMapper;
    private final TaskExecutor taskExecutor;
    private final AccessLogProperties properties;

    public DbAccessLogPersistenceService(SysAccessLogMapper accessLogMapper,
                                         TaskExecutor taskExecutor,
                                         AccessLogProperties properties) {
        this.accessLogMapper = accessLogMapper;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
    }

    @Override
    /**
     * 访问日志默认优先异步落库；线程池不可用时自动降级为同步写入，避免丢日志。
     */
    public void persist(SysAccessLog accessLog) {
        if (accessLog == null || !properties.isPersistEnabled()) {
            return;
        }
        accessLog.setCreateTime(new Date());
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    accessLogMapper.insert(accessLog);
                } catch (Exception e) {
                    log.warn("访问日志落库失败 traceId={} uri={} error={}",
                            accessLog.getTraceId(),
                            accessLog.getRequestUri(),
                            e.getMessage());
                }
            }
        };
        if (properties.isAsyncPersist()) {
            try {
                taskExecutor.execute(task);
                return;
            } catch (RuntimeException e) {
                log.warn("访问日志异步提交失败，改为同步落库 traceId={} error={}",
                        accessLog.getTraceId(),
                        e.getMessage());
            }
        }
        task.run();
    }
}
