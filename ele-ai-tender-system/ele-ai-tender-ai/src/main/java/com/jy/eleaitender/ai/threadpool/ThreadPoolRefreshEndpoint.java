package com.jy.eleaitender.ai.threadpool;

import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 线程池参数刷新Actuator端点
 * 供support模块在参数变更后通知刷新
 */
@Slf4j
@Component
@Endpoint(id = "threadpool")
public class ThreadPoolRefreshEndpoint {

    private final DynamicThreadPoolManager threadPoolManager;
    private final UserConcurrencyManager concurrencyManager;

    public ThreadPoolRefreshEndpoint(DynamicThreadPoolManager threadPoolManager,
                                     UserConcurrencyManager concurrencyManager) {
        this.threadPoolManager = threadPoolManager;
        this.concurrencyManager = concurrencyManager;
    }

    @WriteOperation
    public Map<String, Object> refresh() {
        log.info("收到线程池参数刷新请求");
        try {
            threadPoolManager.refresh();
            concurrencyManager.refresh(threadPoolManager.getProperties());
            ThreadPoolProperties props = threadPoolManager.getProperties();
            return Map.of(
                    "status", "success",
                    "corePoolSize", props.getCorePoolSize(),
                    "maxPoolSize", props.getMaxPoolSize(),
                    "queueCapacity", props.getQueueCapacity(),
                    "globalMaxConcurrent", props.getGlobalMaxConcurrentTasks(),
                    "userMaxConcurrent", props.getUserMaxConcurrentTasks()
            );
        } catch (Exception e) {
            log.error("线程池参数刷新失败", e);
            return Map.of("status", "failed", "error", e.getMessage());
        }
    }
}
