package com.jy.eleaitender.ai.threadpool;

import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import com.jy.eleaitender.ai.mapper.AiTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户级+全局并发控制管理器
 * <p>
 * 发起数（pendingTasks）：查DB统计 PENDING+PROCESSING 数量
 * 并发数（concurrentTasks）：Semaphore控制 PROCESSING 数量
 * <p>
 * 发起数超限 → 直接拒绝
 * 并发数已满 → 排队等待（任务保持PENDING，下次轮询重试）
 */
@Slf4j
@Component
public class UserConcurrencyManager {

    private final AiTaskMapper aiTaskMapper;
    private volatile Semaphore globalSemaphore;
    private volatile ConcurrentHashMap<Long, UserSemaphoreEntry> userSemaphores;
    private volatile ThreadPoolProperties properties;

    public UserConcurrencyManager(AiTaskMapper aiTaskMapper, DynamicThreadPoolManager threadPoolManager) {
        this.aiTaskMapper = aiTaskMapper;
        this.properties = threadPoolManager.getProperties();
        this.globalSemaphore = new Semaphore(properties.getGlobalMaxConcurrentTasks());
        this.userSemaphores = new ConcurrentHashMap<>();
    }

    /**
     * 检查用户/全局发起数是否超限（查DB统计PENDING+PROCESSING数量）
     *
     * @param userId 用户ID
     * @return true=允许发起, false=发起数超限应拒绝
     */
    public boolean tryReserve(Long userId) {
        int globalPending = aiTaskMapper.countPendingGlobal();
        if (globalPending >= properties.getGlobalMaxPendingTasks()) {
            log.warn("全局发起数已达上限: {}/{}", globalPending, properties.getGlobalMaxPendingTasks());
            return false;
        }
        int userPending = aiTaskMapper.countPendingByUserId(userId);
        if (userPending >= properties.getUserMaxPendingTasks()) {
            log.warn("用户{}发起数已达上限: {}/{}", userId, userPending, properties.getUserMaxPendingTasks());
            return false;
        }
        return true;
    }

    /**
     * 尝试获取并发执行许可（Semaphore控制）
     *
     * @param userId 用户ID
     * @return true=获得许可可执行, false=并发数已满应排队
     */
    public boolean tryAcquire(Long userId) {
        UserSemaphoreEntry entry = getOrCreateEntry(userId);
        if (!entry.semaphore().tryAcquire()) {
            log.debug("用户{}并发数已满，排队等待", userId);
            return false;
        }
        if (!globalSemaphore.tryAcquire()) {
            entry.semaphore().release();
            log.debug("全局并发数已满，排队等待");
            return false;
        }
        entry.activeCount().incrementAndGet();
        return true;
    }

    /**
     * 释放并发执行许可
     */
    public void release(Long userId) {
        UserSemaphoreEntry entry = userSemaphores.get(userId);
        if (entry != null) {
            entry.semaphore().release();
            entry.activeCount().decrementAndGet();
        }
        globalSemaphore.release();
    }

    /**
     * 获取当前全局活跃任务数
     */
    public int getGlobalActiveCount() {
        int total = 0;
        for (UserSemaphoreEntry entry : userSemaphores.values()) {
            total += entry.activeCount().get();
        }
        return total;
    }

    /**
     * 热更新并发控制参数
     * 新Semaphore的许可数 = 新上限 - 当前活跃数，保证刷新后并发控制不失效
     */
    public synchronized void refresh(ThreadPoolProperties newProps) {
        int currentGlobalActive = getGlobalActiveCount();
        int newGlobalMax = newProps.getGlobalMaxConcurrentTasks();
        int newGlobalPermits = Math.max(1, newGlobalMax - currentGlobalActive);

        this.properties = newProps;
        this.globalSemaphore = new Semaphore(newGlobalPermits);
        this.userSemaphores = new ConcurrentHashMap<>();

        log.info("并发控制参数已刷新: globalMax={}, userMax={}, currentActive={}, newPermits={}",
                newGlobalMax, newProps.getUserMaxConcurrentTasks(), currentGlobalActive, newGlobalPermits);
    }

    private UserSemaphoreEntry getOrCreateEntry(Long userId) {
        return userSemaphores.computeIfAbsent(userId,
                id -> new UserSemaphoreEntry(
                        new Semaphore(properties.getUserMaxConcurrentTasks()),
                        new AtomicInteger(0)));
    }

    /**
     * 用户Semaphore条目，包含信号量和活跃计数
     */
    private record UserSemaphoreEntry(Semaphore semaphore, AtomicInteger activeCount) {
    }
}
