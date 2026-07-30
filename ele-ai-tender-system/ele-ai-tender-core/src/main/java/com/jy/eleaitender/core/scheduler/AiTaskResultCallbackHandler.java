package com.jy.eleaitender.core.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import com.jy.eleaitender.common.exception.AiSyncedException;
import com.jy.eleaitender.common.interaction.dto.AiTaskResultCallbackRequest;
import com.jy.eleaitender.common.util.SignatureUtil;
import com.jy.eleaitender.core.mapper.SysAccessSystemQueryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

/**
 * AI任务回调推送处理器
 * 签名 → HTTP推送 → 更新回调状态
 */
@Slf4j
@Service
public class AiTaskResultCallbackHandler {

    private static final String CALLBACK_PATH = "/api/eleAiTender/interaction/callbacks/ai-task-result";

    @Autowired
    private SysAccessSystemQueryMapper accessSystemQueryMapper;

    @Autowired
    @Qualifier("callbackRestTemplate")
    private RestTemplate callbackRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 处理待回调记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void callback(AiTask task) {
        // 3. 查询外部系统信息
        SysAccessSystem system = accessSystemQueryMapper.selectById(task.getSystemId());
        if (system == null || !StringUtils.hasText(system.getSystemUrl())) {
            throw new AiSyncedException("外部系统不存在或未配置system_url");
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
        request.setCompletedAt(formatDate(task.getCompletedAt()));

        // 5. 签名并推送
        String callbackUrl = buildCallbackUrl(system.getSystemUrl());
        try {
            long timestamp = System.currentTimeMillis();
            String signature = SignatureUtil.generateSignature(system.getAppKey(), timestamp, system.getAppSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("X-App-Key", system.getAppKey());
            headers.set("X-Timestamp", String.valueOf(timestamp));
            headers.set("X-Signature", signature);

            log.info("回调推送请求: taskId={}, url={}", task.getId(), callbackUrl);

            // 发送POST请求
            post(callbackUrl, headers, request);

            // 推送成功
            log.info("回调推送成功: taskId={}, url={}", task.getId(), callbackUrl);
        } catch (Exception e) {
            log.error("回调推送失败: taskId={}, url={}, error={}", task.getId(), callbackUrl, e.getMessage(), e);
            throw new AiSyncedException("回调推送失败");
        }
    }

    private void post(String url, HttpHeaders headers, Object body) throws JsonProcessingException {
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> responseEntity = callbackRestTemplate.postForEntity(url, entity, String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new RestClientException("回调推送失败");
        }
        JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody());
        int code = jsonNode.get("code").asInt();
        if (code != 200) {
            String msg = "";
            if (jsonNode.get("msg") != null) {
                msg = jsonNode.get("msg").asText();
            }
            if (jsonNode.get("message") != null) {
                msg = jsonNode.get("message").asText();
            }
            throw new RestClientException(msg);
        }
    }

    private String buildCallbackUrl(String systemUrl) {
        String base = systemUrl.endsWith("/") ? systemUrl.substring(0, systemUrl.length() - 1) : systemUrl;
        return base + CALLBACK_PATH;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
