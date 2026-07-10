package com.jy.eleaitender.core.scheduler;

import com.jy.eleaitender.core.service.external.AiTaskCallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI任务回调定时调度器
 * 定期扫描PENDING回调记录并推送
 */
@Slf4j
@Component
public class AiTaskCallbackScheduler {

    @Autowired
    private AiTaskCallbackService aiTaskCallbackService;

    @Value("${ele-ai-tender.external.ai-task.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${ele-ai-tender.external.ai-task.callback-schedule-interval:10000}")
    public void scheduleCallback() {
        if (!enabled) {
            return;
        }
        try {
            aiTaskCallbackService.processPendingCallbacks();
        } catch (Exception e) {
            log.error("回调调度异常", e);
        }
    }
}
