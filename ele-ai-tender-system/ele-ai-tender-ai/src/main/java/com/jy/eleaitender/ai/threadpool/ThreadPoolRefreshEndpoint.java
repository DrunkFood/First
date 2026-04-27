package com.jy.eleaitender.ai.threadpool;

import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import com.jy.eleaitender.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 线程池参数刷新接口
 * 供support模块在参数变更后通知刷新
 * 路径在/api/下，受JWT认证保护
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/threadpool")
public class ThreadPoolRefreshEndpoint {

    private final DynamicThreadPoolManager threadPoolManager;
    private final UserConcurrencyManager concurrencyManager;

    public ThreadPoolRefreshEndpoint(DynamicThreadPoolManager threadPoolManager,
                                     UserConcurrencyManager concurrencyManager) {
        this.threadPoolManager = threadPoolManager;
        this.concurrencyManager = concurrencyManager;
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh() {
        log.info("收到线程池参数刷新请求");
        try {
            threadPoolManager.refresh();
            concurrencyManager.refresh(threadPoolManager.getProperties());
            ThreadPoolProperties props = threadPoolManager.getProperties();
            Map<String, Object> data = Map.of(
                    "corePoolSize", props.getCorePoolSize(),
                    "maxPoolSize", props.getMaxPoolSize(),
                    "queueCapacity", props.getQueueCapacity(),
                    "globalMaxConcurrent", props.getGlobalMaxConcurrentTasks(),
                    "userMaxConcurrent", props.getUserMaxConcurrentTasks()
            );
            return Result.success(data);
        } catch (Exception e) {
            log.error("线程池参数刷新失败", e);
            return Result.fail(500, "线程池参数刷新失败: " + e.getMessage());
        }
    }
}
