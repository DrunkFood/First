package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.dto.PolicyFileQueryResponse;
import com.jy.eleaitender.common.interaction.util.InteractionResultExtractor;
import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionTraceSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * 政策文件客户端 — 封装政策文件查询能力。
 * <p>外部系统需先通过 {@link AiExternalAuthClient#getExternalToken} 获取JWT令牌，
 * 再将令牌传入本类各方法的 {@code authorization} 参数。</p>
 */
@Slf4j
public class AiPolicyFileClient {

    private final RestTemplate restTemplate;
    private final EleAiTenderInteractionProperties properties;

    public AiPolicyFileClient(RestTemplate restTemplate,
                              EleAiTenderInteractionProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 查询政策文件
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     */
    public PolicyFileQueryResponse getPolicyFile(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = properties.getCoreBaseUrl() + properties.getPolicyFileQueryPath();
        ResponseEntity<InteractionResult<PolicyFileQueryResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<InteractionResult<PolicyFileQueryResponse>>() {
                });
        PolicyFileQueryResponse data = InteractionResultExtractor.extractData(response.getBody(), "查询政策文件失败");
        log.info("INTERACTION LOCAL traceId={} api=ai-task/query success=true", InteractionTraceSupport.getTraceId());
        return data;
    }
}
