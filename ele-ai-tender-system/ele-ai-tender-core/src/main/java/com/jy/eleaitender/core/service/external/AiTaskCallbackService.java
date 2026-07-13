package com.jy.eleaitender.core.service.external;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import com.jy.eleaitender.common.enums.AiTaskStatus;
import com.jy.eleaitender.common.interaction.dto.AiTaskResultCallbackRequest;
import com.jy.eleaitender.common.util.SignatureUtil;
import com.jy.eleaitender.common.entity.ai.AiTaskExternalCallback;
import com.jy.eleaitender.core.mapper.AiTaskExternalCallbackMapper;
import com.jy.eleaitender.core.mapper.AiTaskMapper;
import com.jy.eleaitender.core.mapper.SysAccessSystemQueryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * AI任务回调推送服务
 * 查询终态任务 → 签名 → HTTP推送 → 更新回调状态
 */
@Slf4j
@Service
public class AiTaskCallbackService {

    private static final String CALLBACK_PATH = "/api/eleAiTender/interaction/callbacks/ai-task-result";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AiTaskExternalCallbackMapper callbackMapper;

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private SysAccessSystemQueryMapper accessSystemQueryMapper;

    @Autowired
    @Qualifier("callbackRestTemplate")
    private RestTemplate callbackRestTemplate;

    @Value("${ele-ai-tender.external.ai-task.callback-max-retry:3}")
    private int maxRetry;

    /**
     * 处理待回调记录
     */
    public void processPendingCallbacks() {
        List<AiTaskExternalCallback> pendingList = callbackMapper.selectPendingCallbacks(50);
        if (pendingList.isEmpty()) {
            return;
        }
        log.debug("待回调记录: {}条", pendingList.size());

        for (AiTaskExternalCallback callback : pendingList) {
            try {
                processSingleCallback(callback);
            } catch (Exception e) {
                log.error("回调处理异常: callbackId={}, taskId={}", callback.getId(), callback.getTaskId(), e);
            }
        }
    }

    private void processSingleCallback(AiTaskExternalCallback callback) {
        // 0. CAS加锁：防止多实例重复处理
        int locked = callbackMapper.casUpdateToProcessing(callback.getId());
        if (locked == 0) {
            log.debug("回调记录已被其他实例处理: callbackId={}", callback.getId());
            return;
        }

        // 1. 查询任务
        AiTask task = aiTaskMapper.selectById(callback.getTaskId());
        if (task == null) {
            log.warn("回调任务不存在: taskId={}", callback.getTaskId());
            markFailed(callback, "任务不存在");
            return;
        }

        // 2. 检查是否终态
        AiTaskStatus taskStatus = AiTaskStatus.fromCode(task.getStatus());
        if (!taskStatus.isTerminal()) {
            // 非终态，重置为PENDING等待下一轮调度（避免卡死在PROCESSING）
            callback.setCallbackStatus("PENDING");
            callbackMapper.updateById(callback);
            log.debug("任务未到终态，重置回调为PENDING: callbackId={}, taskId={}, taskStatus={}",
                    callback.getId(), callback.getTaskId(), taskStatus);
            return;
        }

        // 3. 查询外部系统信息
        SysAccessSystem system = accessSystemQueryMapper.selectByAppKey(callback.getAppKey());
        if (system == null || !StringUtils.hasText(system.getSystemUrl())) {
            markFailed(callback, "外部系统不存在或未配置system_url");
            return;
        }

        // 4. 构建回调请求
        AiTaskResultCallbackRequest request = new AiTaskResultCallbackRequest();
        request.setTaskId(task.getId());
        request.setTaskType(task.getTaskType());
        request.setBizId(task.getBizId());
        request.setBizType(task.getBizType());
        request.setStatus(task.getStatus());
        request.setResult(task.getResult());
        request.setErrorMsg(task.getErrorMsg());
        request.setCompletedAt(formatLocalDateTime(task.getCompletedAt()));

        // 5. 签名并推送
        String callbackUrl = buildCallbackUrl(system.getSystemUrl());
        try {
            long timestamp = System.currentTimeMillis();
            String signature = SignatureUtil.generateSignature(callback.getAppKey(), timestamp, system.getAppSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("X-App-Key", callback.getAppKey());
            headers.set("X-Timestamp", String.valueOf(timestamp));
            headers.set("X-Signature", signature);

            HttpEntity<AiTaskResultCallbackRequest> entity = new HttpEntity<>(request, headers);
            callbackRestTemplate.postForEntity(callbackUrl, entity, String.class);

            // 推送成功
            markSuccess(callback);
            log.info("回调推送成功: taskId={}, url={}", task.getId(), callbackUrl);
        } catch (Exception e) {
            log.warn("回调推送失败: taskId={}, url={}, error={}", task.getId(), callbackUrl, e.getMessage());
            markRetryOrFail(callback, e.getMessage());
        }
    }

    private String buildCallbackUrl(String systemUrl) {
        String base = systemUrl.endsWith("/") ? systemUrl.substring(0, systemUrl.length() - 1) : systemUrl;
        return base + CALLBACK_PATH;
    }

    private void markSuccess(AiTaskExternalCallback callback) {
        callback.setCallbackStatus("SUCCESS");
        callback.setLastCallbackTime(LocalDateTime.now());
        callback.setErrorMsg(null);
        int rows = callbackMapper.updateById(callback);
        if (rows == 0) {
            log.warn("回调状态更新失败(乐观锁冲突): callbackId={}, taskId={}", callback.getId(), callback.getTaskId());
        }
    }

    private void markFailed(AiTaskExternalCallback callback, String errorMsg) {
        callback.setCallbackStatus("FAILED");
        callback.setLastCallbackTime(LocalDateTime.now());
        callback.setErrorMsg(errorMsg);
        int rows = callbackMapper.updateById(callback);
        if (rows == 0) {
            log.warn("回调状态更新失败(乐观锁冲突): callbackId={}, taskId={}", callback.getId(), callback.getTaskId());
        }
    }

    private void markRetryOrFail(AiTaskExternalCallback callback, String errorMsg) {
        int newRetryCount = (callback.getRetryCount() != null ? callback.getRetryCount() : 0) + 1;
        callback.setRetryCount(newRetryCount);
        callback.setLastCallbackTime(LocalDateTime.now());
        callback.setErrorMsg(errorMsg);

        if (newRetryCount >= maxRetry) {
            callback.setCallbackStatus("FAILED");
            log.warn("回调达到最大重试次数，标记失败: callbackId={}, taskId={}, retryCount={}",
                    callback.getId(), callback.getTaskId(), newRetryCount);
        } else {
            // 未达到最大重试次数，重置为PENDING等待下一轮调度
            callback.setCallbackStatus("PENDING");
        }
        int rows = callbackMapper.updateById(callback);
        if (rows == 0) {
            log.warn("回调状态更新失败(乐观锁冲突): callbackId={}, taskId={}", callback.getId(), callback.getTaskId());
        }
    }

    private String formatLocalDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : null;
    }
}
