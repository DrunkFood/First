package com.jy.eleaitender.ai.threadpool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import com.jy.eleaitender.ai.mapper.SysParameterReadMapper;
import com.jy.eleaitender.common.entity.support.SysParameter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 动态线程池管理器
 * 支持从DB加载参数、运行时热更新
 * 通过定时轮询DB参数Hash变更自动刷新
 */
@Slf4j
@Component
public class DynamicThreadPoolManager {

    @Autowired
    private SysParameterReadMapper sysParameterReadMapper;

    @Lazy
    @Autowired
    private UserConcurrencyManager concurrencyManager;

    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private volatile ThreadPoolExecutor executor;
    @Getter
    private volatile ThreadPoolProperties properties;
    private volatile String cachedParamHash;

    @PostConstruct
    public void init() {
        this.properties = loadFromDb();
        this.cachedParamHash = computeHash(this.properties);
        this.executor = createExecutor(properties);
        log.info("动态线程池初始化完成: core={}, max={}, queue={}, keepAlive={}s",
                properties.getCorePoolSize(), properties.getMaxPoolSize(),
                properties.getQueueCapacity(), properties.getKeepAliveSeconds());
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
            log.info("动态线程池已关闭");
        }
    }

    /**
     * 每10秒轮询DB参数，Hash变更则自动刷新线程池和并发控制
     */
    @Scheduled(fixedDelay = 10000)
    public void checkAndRefreshIfNeeded() {
        try {
            ThreadPoolProperties newProps = loadFromDb();
            String newHash = computeHash(newProps);
            if (newHash.equals(cachedParamHash)) {
                return;
            }
            log.info("检测到AI线程池参数变更, hash: {} -> {}", cachedParamHash, newHash);
            applyRefresh(newProps);
            this.cachedParamHash = newHash;
        } catch (Exception e) {
            log.error("轮询线程池参数变更失败", e);
        }
    }

    /**
     * 提交任务到线程池，返回Future（带超时控制）
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * 提交无返回值任务
     */
    public void execute(Runnable task) {
        executor.execute(task);
    }

    public int getActiveCount() {
        return executor != null ? executor.getActiveCount() : 0;
    }

    public int getPoolSize() {
        return executor != null ? executor.getPoolSize() : 0;
    }

    public int getQueueSize() {
        return executor != null ? executor.getQueue().size() : 0;
    }

    /**
     * 应用参数刷新到线程池和并发控制
     */
    private void applyRefresh(ThreadPoolProperties newProps) {
        ThreadPoolProperties oldProps = this.properties;

        // 队列容量变更需要重建线程池
        if (newProps.getQueueCapacity() != oldProps.getQueueCapacity()) {
            log.info("队列容量变更 {} -> {}，重建线程池", oldProps.getQueueCapacity(), newProps.getQueueCapacity());
            ThreadPoolExecutor oldExecutor = this.executor;
            this.executor = createExecutor(newProps);
            oldExecutor.shutdown();
            try {
                if (!oldExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    oldExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                oldExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } else {
            executor.setCorePoolSize(newProps.getCorePoolSize());
            executor.setMaximumPoolSize(newProps.getMaxPoolSize());
            executor.setKeepAliveTime(newProps.getKeepAliveSeconds(), TimeUnit.SECONDS);
        }
        this.properties = newProps;

        // 热更新并发控制参数
        concurrencyManager.refresh(newProps);

        log.info("线程池参数已刷新: core={}, max={}, queue={}, keepAlive={}s, timeout={}min",
                newProps.getCorePoolSize(), newProps.getMaxPoolSize(),
                newProps.getQueueCapacity(), newProps.getKeepAliveSeconds(),
                newProps.getTaskTimeoutMinutes());
    }

    private ThreadPoolExecutor createExecutor(ThreadPoolProperties props) {
        return new ThreadPoolExecutor(
                props.getCorePoolSize(),
                props.getMaxPoolSize(),
                props.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(props.getQueueCapacity()),
                r -> {
                    Thread t = new Thread(r, "ai-task-pool-" + threadCounter.incrementAndGet());
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 从sup_sys_parameter表读取AI_THREAD_POOL组参数
     */
    private ThreadPoolProperties loadFromDb() {
        LambdaQueryWrapper<SysParameter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysParameter::getParamGroup, "AI_THREAD_POOL");
        List<SysParameter> sysParameters = sysParameterReadMapper.selectList(wrapper);

        Map<String, String> paramMap = sysParameters.stream().collect(Collectors.toMap(
                SysParameter::getParamKey,
                SysParameter::getParamValue
        ));

        ThreadPoolProperties props = new ThreadPoolProperties();
        props.setCorePoolSize(getInt(paramMap, "global_max_concurrent_tasks", 10));
        props.setMaxPoolSize(getInt(paramMap, "global_max_concurrent_tasks", 10));
        props.setQueueCapacity(getInt(paramMap, "global_max_pending_tasks", 100));
        props.setTaskTimeoutMinutes(getInt(paramMap, "ai_task_timeout_minutes", 10));
        props.setUserMaxPendingTasks(getInt(paramMap, "user_max_pending_tasks", 5));
        props.setUserMaxConcurrentTasks(getInt(paramMap, "user_max_concurrent_tasks", 2));
        props.setGlobalMaxPendingTasks(getInt(paramMap, "global_max_pending_tasks", 100));
        props.setGlobalMaxConcurrentTasks(getInt(paramMap, "global_max_concurrent_tasks", 10));

        return props;
    }

    /**
     * 计算参数Hash（所有key=value拼接后取hashCode）
     */
    private String computeHash(ThreadPoolProperties props) {
        String raw = String.join("|",
                "core=" + props.getCorePoolSize(),
                "max=" + props.getMaxPoolSize(),
                "queue=" + props.getQueueCapacity(),
                "keepAlive=" + props.getKeepAliveSeconds(),
                "timeout=" + props.getTaskTimeoutMinutes(),
                "userMaxPending=" + props.getUserMaxPendingTasks(),
                "userMaxConcurrent=" + props.getUserMaxConcurrentTasks(),
                "globalMaxPending=" + props.getGlobalMaxPendingTasks(),
                "globalMaxConcurrent=" + props.getGlobalMaxConcurrentTasks()
        );
        return Integer.toHexString(raw.hashCode());
    }

    private int getInt(Map<String, String> map, String key, int defaultValue) {
        String value = map.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("参数{}值无效: {}, 使用默认值: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
