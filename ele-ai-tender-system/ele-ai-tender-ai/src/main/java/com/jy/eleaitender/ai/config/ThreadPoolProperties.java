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

    /**
     * 计算参数Hash（所有key=value拼接后取hashCode）
     */
    public String computeHash() {
        String raw = String.join("|",
                "core=" + this.getCorePoolSize(),
                "max=" + this.getMaxPoolSize(),
                "queue=" + this.getQueueCapacity(),
                "keepAlive=" + this.getKeepAliveSeconds(),
                "timeout=" + this.getTaskTimeoutMinutes(),
                "userMaxPending=" + this.getUserMaxPendingTasks(),
                "userMaxConcurrent=" + this.getUserMaxConcurrentTasks(),
                "globalMaxPending=" + this.getGlobalMaxPendingTasks(),
                "globalMaxConcurrent=" + this.getGlobalMaxConcurrentTasks()
        );
        return Integer.toHexString(raw.hashCode());
    }

}
