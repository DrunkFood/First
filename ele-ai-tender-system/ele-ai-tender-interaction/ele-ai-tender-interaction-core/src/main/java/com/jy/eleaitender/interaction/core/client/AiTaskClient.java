package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.AiTaskCreateRequest;
import com.jy.eleaitender.common.interaction.dto.AiTaskCreateResponse;
import com.jy.eleaitender.common.interaction.dto.AiTaskQueryResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.util.InteractionResultExtractor;
import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionTraceSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * AI任务客户端 — 封装AI任务创建、查询和状态查询能力。
 * <p>外部系统需先通过 {@link AiExternalAuthClient#getExternalToken} 获取JWT令牌，
 * 再将令牌传入本类各方法的 {@code authorization} 参数。</p>
 */
@Slf4j
public class AiTaskClient {

    private final RestTemplate restTemplate;
    private final EleAiTenderInteractionProperties properties;

    public AiTaskClient(RestTemplate restTemplate,
                        EleAiTenderInteractionProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 创建AI任务
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     */
    public AiTaskCreateResponse createTask(String authorization, AiTaskCreateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<AiTaskCreateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<InteractionResult<AiTaskCreateResponse>> response = restTemplate.exchange(
                properties.getCoreBaseUrl() + properties.getAiTaskCreatePath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<AiTaskCreateResponse>>() {
                });
        AiTaskCreateResponse data = InteractionResultExtractor.extractData(response.getBody(), "创建AI任务失败");
        log.info("INTERACTION LOCAL traceId={} api=ai-task/create success=true taskId={} taskType={} status={}",
                InteractionTraceSupport.getTraceId(),
                data.getTaskId(),
                data.getTaskType(),
                data.getStatus());
        return data;
    }

    /**
     * 查询AI任务详情
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     */
    public AiTaskQueryResponse getTask(String authorization, Long taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = properties.getCoreBaseUrl() + properties.getAiTaskQueryPath().replace("{taskId}", String.valueOf(taskId));
        ResponseEntity<InteractionResult<AiTaskQueryResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<InteractionResult<AiTaskQueryResponse>>() {
                });
        AiTaskQueryResponse data = InteractionResultExtractor.extractData(response.getBody(), "查询AI任务失败");
        log.info("INTERACTION LOCAL traceId={} api=ai-task/query success=true taskId={} status={}",
                InteractionTraceSupport.getTraceId(),
                data.getTaskId(),
                data.getStatus());
        return data;
    }

    /**
     * 查询AI任务状态
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     */
    public AiTaskQueryResponse getTaskStatus(String authorization, Long taskId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = properties.getCoreBaseUrl() + properties.getAiTaskStatusPath().replace("{taskId}", String.valueOf(taskId));
        ResponseEntity<InteractionResult<AiTaskQueryResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<InteractionResult<AiTaskQueryResponse>>() {
                });
        AiTaskQueryResponse data = InteractionResultExtractor.extractData(response.getBody(), "查询AI任务状态失败");
        log.info("INTERACTION LOCAL traceId={} api=ai-task/status success=true taskId={} status={}",
                InteractionTraceSupport.getTraceId(),
                data.getTaskId(),
                data.getStatus());
        return data;
    }
}
