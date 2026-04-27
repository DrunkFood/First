package com.jy.eleaitender.support.notifier;

import com.jy.eleaitender.support.client.InternalAiServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 线程池参数变更通知器
 * 当AI_THREAD_POOL组参数被修改时，通知AI模块刷新
 */
@Slf4j
@Component
public class ThreadPoolConfigNotifier {

    @Autowired(required = false)
    private InternalAiServiceClient internalAiServiceClient;

    /**
     * 通知AI模块刷新线程池参数
     * @param paramGroup 变更的参数分组
     */
    public void onParameterChanged(String paramGroup) {
        if (!"AI_THREAD_POOL".equals(paramGroup)) {
            return;
        }
        if (internalAiServiceClient == null) {
            log.warn("AI服务客户端未配置，跳过线程池刷新通知");
            return;
        }
        try {
            boolean success = internalAiServiceClient.notifyRefreshThreadPool();
            if (success) {
                log.info("AI线程池参数刷新通知发送成功");
            } else {
                log.warn("AI线程池参数刷新通知发送失败");
            }
        } catch (Exception e) {
            log.error("AI线程池参数刷新通知异常: {}", e.getMessage());
        }
    }
}
