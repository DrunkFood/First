package com.jy.eleaitender.ai.threadpool;

import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import com.jy.eleaitender.ai.mapper.AiTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 用户级+全局并发控制管理器
 *
 * 发起数（pendingTasks）：查DB统计 PENDING+PROCESSING 数量
 * 并发数（concurrentTasks）：Semaphore控制 PROCESSING 数量
 *
 * 发起数超限 → 直接拒绝
 * 并发数已满 → 排队等待（任务保持PENDING，下次轮询重试）
 */
@Slf4j
@Component
public class UserConcurrencyManager {

    private final AiTaskMapper aiTaskMapper;
    private volatile Semaphore globalSemaphore;
    private volatile ConcurrentHashMap<Long, Semaphore> userSemaphores;
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
        // 检查全局发起数
        int globalPending = aiTaskMapper.countPendingGlobal();
        if (globalPending >= properties.getGlobalMaxPendingTasks()) {
            log.warn("全局发起数已达上限: {}/{}", globalPending, properties.getGlobalMaxPendingTasks());
            return false;
        }
        // 检查用户发起数
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
        Semaphore userSemaphore = getOrCreateUserSemaphore(userId);
        // 非阻塞尝试获取：先尝试用户许可，再尝试全局许可
        if (!userSemaphore.tryAcquire()) {
            log.debug("用户{}并发数已满，排队等待", userId);
            return false;
        }
        if (!globalSemaphore.tryAcquire()) {
            // 全局已满，归还用户许可
            userSemaphore.release();
            log.debug("全局并发数已满，排队等待");
            return false;
        }
        return true;
    }

    /**
     * 释放并发执行许可
     */
    public void release(Long userId) {
        Semaphore userSemaphore = userSemaphores.get(userId);
        if (userSemaphore != null) {
            userSemaphore.release();
        }
        globalSemaphore.release();
    }

    /**
     * 热更新并发控制参数
     * 重建Semaphore，已获得的许可不强制回收
     */
    public synchronized void refresh(ThreadPoolProperties newProps) {
        this.properties = newProps;
        // 重建全局Semaphore
        this.globalSemaphore = new Semaphore(newProps.getGlobalMaxConcurrentTasks());
        // 清空用户Semaphore映射，下次使用时按新参数创建
        this.userSemaphores = new ConcurrentHashMap<>();
        log.info("并发控制参数已刷新: globalMax={}, userMax={}",
                newProps.getGlobalMaxConcurrentTasks(), newProps.getUserMaxConcurrentTasks());
    }

    private Semaphore getOrCreateUserSemaphore(Long userId) {
        return userSemaphores.computeIfAbsent(userId,
                id -> new Semaphore(properties.getUserMaxConcurrentTasks()));
    }
}
