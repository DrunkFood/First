package com.jy.eleaitender.core.scheduler;

import com.jy.eleaitender.core.service.IAiTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI任务超时检查定时任务
 */
@Slf4j
@Component
public class AiTaskTimeoutChecker {

    @Autowired
    private IAiTaskService aiTaskService;

    /**
     * 每分钟检查超时任务
     */
    @Scheduled(fixedRate = 60000)
    public void checkTimeoutTasks() {
        try {
            int count = aiTaskService.markTimeoutTasks();
            if (count > 0) {
                log.info("超时检查: 标记{}个任务为AI_UNAVAILABLE", count);
            }
        } catch (Exception e) {
            log.error("超时检查异常", e);
        }
    }
}
