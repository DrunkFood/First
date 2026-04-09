package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.ExternalUserInfoResponse;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.util.InteractionResultExtractor;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eletender.interaction.core.support.InteractionLogMasker;
import com.jy.eletender.interaction.core.support.InteractionRequestSigner;
import com.jy.eletender.interaction.core.support.InteractionTraceSupport;
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
 * 外部用户信息客户端
 */
@Slf4j
public class ExternalUserInfoClient {

    private final RestTemplate restTemplate;
    private final EleTenderInteractionProperties properties;
    private final InteractionRequestSigner signer;

    public ExternalUserInfoClient(RestTemplate restTemplate,
                                  EleTenderInteractionProperties properties,
                                  InteractionRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.signer = signer;
    }

    /**
     * 调用电子标系统 `/external/userinfo`，并透传当前 Authorization 获取外部用户信息。
     */
    public ExternalUserInfoResponse getCurrentExternalUser(String authorization) {
        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<InteractionResult<ExternalUserInfoResponse>> response = restTemplate.exchange(
                properties.getApiBaseUrl() + properties.getUserInfoPath(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<InteractionResult<ExternalUserInfoResponse>>() {
                });
        ExternalUserInfoResponse data = InteractionResultExtractor.extractData(response.getBody(), "获取外部用户信息失败");
        log.info("INTERACTION LOCAL traceId={} api=external/userinfo success=true userId={} userName={} enterpriseId={} enterpriseName={} appKey={} authorization={}",
                InteractionTraceSupport.getTraceId(),
                data.getUserId(),
                data.getUserName(),
                data.getEnterpriseId(),
                data.getEnterpriseName(),
                data.getAppKey(),
                InteractionLogMasker.maskToken(authorization));
        return data;
    }
}
