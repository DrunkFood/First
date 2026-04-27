package com.jy.eleaitender.ai.config;

import lombok.Data;

/**
 * AI线程池动态参数（从sup_sys_parameter表加载）
 */
@Data
public class ThreadPoolProperties {

    private int corePoolSize = 4;
    private int maxPoolSize = 8;
    private int queueCapacity = 20;
    private int keepAliveSeconds = 60;
    private int taskTimeoutMinutes = 10;
    private int userMaxPendingTasks = 5;
    private int userMaxConcurrentTasks = 2;
    private int globalMaxPendingTasks = 100;
    private int globalMaxConcurrentTasks = 10;
}
