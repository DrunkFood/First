package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eleaitender.common.interaction.dto.ExternalTokenResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.util.InteractionResultExtractor;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionLogMasker;
import com.jy.eleaitender.interaction.core.support.InteractionRequestSigner;
import com.jy.eleaitender.interaction.core.support.InteractionTraceSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * 外部认证客户端
 */
@Slf4j
public class AiExternalAuthClient {

    private final RestTemplate restTemplate;
    private final EleAiTenderInteractionProperties properties;
    private final InteractionRequestSigner signer;

    public AiExternalAuthClient(RestTemplate restTemplate,
                                EleAiTenderInteractionProperties properties,
                                InteractionRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.signer = signer;
    }

    /**
     * 调用电子标系统 `/external/token`，为业务系统用户换取可跳转使用的外部 token。
     */
    public ExternalTokenResponse getExternalToken(ExternalTokenRequest request) {
        InteractionValidationUtils.validateExternalTokenRequest(request);
        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<ExternalTokenRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<InteractionResult<ExternalTokenResponse>> response = restTemplate.exchange(
                properties.getApiBaseUrl() + properties.getTokenPath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<ExternalTokenResponse>>() {
                });
        // 统一在客户端边界把 Result<T> 解包，接入方只关心真实数据或异常。
        ExternalTokenResponse data = InteractionResultExtractor.extractData(response.getBody(), "获取电子标Token失败");
        log.info("INTERACTION LOCAL traceId={} api=external/token success=true userId={} userName={} enterpriseId={} enterpriseName={} token={} expireIn={}",
                InteractionTraceSupport.getTraceId(),
                request.getUserId(),
                request.getUserName(),
                request.getEnterpriseId(),
                request.getEnterpriseName(),
                InteractionLogMasker.maskToken(data.getToken()),
                data.getExpireIn());
        return data;
    }
}
