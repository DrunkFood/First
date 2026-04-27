package com.jy.eleaitender.ai.threadpool;

import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 动态线程池管理器
 * 支持从DB加载参数、运行时热更新
 */
@Slf4j
@Component
public class DynamicThreadPoolManager {

    private final JdbcTemplate jdbcTemplate;
    private volatile ThreadPoolExecutor executor;
    private volatile ThreadPoolProperties properties;

    public DynamicThreadPoolManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        this.properties = loadFromDb();
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

    /**
     * 热更新线程池参数
     */
    public synchronized void refresh() {
        ThreadPoolProperties newProps = loadFromDb();
        ThreadPoolProperties oldProps = this.properties;

        // 队列容量变更需要重建线程池
        if (newProps.getQueueCapacity() != oldProps.getQueueCapacity()) {
            log.info("队列容量变更 {} -> {}，重建线程池", oldProps.getQueueCapacity(), newProps.getQueueCapacity());
            ThreadPoolExecutor oldExecutor = this.executor;
            this.executor = createExecutor(newProps);
            // 优雅关闭旧线程池：等待已提交任务完成
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
            // JDK原生支持热更新
            executor.setCorePoolSize(newProps.getCorePoolSize());
            executor.setMaximumPoolSize(newProps.getMaxPoolSize());
            executor.setKeepAliveTime(newProps.getKeepAliveSeconds(), TimeUnit.SECONDS);
        }

        this.properties = newProps;
        log.info("线程池参数已刷新: core={}, max={}, queue={}, keepAlive={}s, timeout={}min",
                newProps.getCorePoolSize(), newProps.getMaxPoolSize(),
                newProps.getQueueCapacity(), newProps.getKeepAliveSeconds(),
                newProps.getTaskTimeoutMinutes());
    }

    public ThreadPoolProperties getProperties() {
        return properties;
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

    private ThreadPoolExecutor createExecutor(ThreadPoolProperties props) {
        return new ThreadPoolExecutor(
                props.getCorePoolSize(),
                props.getMaxPoolSize(),
                props.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(props.getQueueCapacity()),
                r -> {
                    Thread t = new Thread(r, "ai-task-pool-" + System.nanoTime());
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
        ThreadPoolProperties props = new ThreadPoolProperties();
        String sql = "SELECT param_key, param_value FROM sup_sys_parameter " +
                "WHERE param_group = 'AI_THREAD_POOL' AND is_delete = 0";
        Map<String, String> paramMap = jdbcTemplate.query(sql, rs -> {
            Map<String, String> map = new ConcurrentHashMap<>();
            while (rs.next()) {
                map.put(rs.getString("param_key"), rs.getString("param_value"));
            }
            return map;
        });

        props.setCorePoolSize(getInt(paramMap, "ai_core_pool_size", 4));
        props.setMaxPoolSize(getInt(paramMap, "ai_max_pool_size", 8));
        props.setQueueCapacity(getInt(paramMap, "ai_queue_capacity", 20));
        props.setKeepAliveSeconds(getInt(paramMap, "ai_keep_alive_seconds", 60));
        props.setTaskTimeoutMinutes(getInt(paramMap, "ai_task_timeout_minutes", 10));
        props.setUserMaxPendingTasks(getInt(paramMap, "user_max_pending_tasks", 5));
        props.setUserMaxConcurrentTasks(getInt(paramMap, "user_max_concurrent_tasks", 2));
        props.setGlobalMaxPendingTasks(getInt(paramMap, "global_max_pending_tasks", 100));
        props.setGlobalMaxConcurrentTasks(getInt(paramMap, "global_max_concurrent_tasks", 10));

        return props;
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
