package com.jy.eleaitender.core.scheduler;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.core.mapper.AiTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI任务结果同步调度器
 * 定期扫描已完成的AI任务，将结果同步到对应的业务表
 */
@Slf4j
@Component
public class AiTaskResultSyncScheduler {

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private AiTaskResultSyncHandler syncHandler;

    @Autowired
    private AiTaskResultCallbackHandler callbackService;

    /**
     * 每10秒扫描未同步的终态AI任务
     */
    @Scheduled(fixedDelay = 10000)
    public void syncCompletedTasks() {
        List<AiTask> tasks;
        try {
            tasks = aiTaskMapper.selectUnsyncedTasks(20);
        } catch (Exception e) {
            log.error("查询未同步AI任务失败", e);
            return;
        }

        if (tasks.isEmpty()) {
            return;
        }

        log.info("发现{}个未同步AI任务", tasks.size());

        for (AiTask task : tasks) {
            try {
                if (aiTaskMapper.markSyncing(task.getId()) == 0) {
                    log.debug("AI任务结果已被其他实例抢占，跳过同步: id={}, type={}", task.getId(), task.getTaskType());
                    continue;
                }
                if (task.getSystemId() == null || task.getSystemId() == 0L) {
                    syncHandler.sync(task);
                } else {
                    callbackService.callback(task);
                }
                aiTaskMapper.markSuccessSynced(task.getId());
            } catch (Exception e) {
                log.error("同步AI任务结果失败: id={}, type={}", task.getId(), task.getTaskType(), e);
                try {
                    aiTaskMapper.markFailedSynced(task.getId(), e.getMessage());
                } catch (Exception ex) {
                    log.error("标记同步失败状态异常: id={}", task.getId(), ex);
                }
            }
        }
    }
}
