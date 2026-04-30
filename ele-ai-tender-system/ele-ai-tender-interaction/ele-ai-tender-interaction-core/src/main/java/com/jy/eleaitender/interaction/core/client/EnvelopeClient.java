package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eleaitender.common.interaction.dto.EnvelopeJoinResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.util.InteractionResultExtractor;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionRequestSigner;
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
 * 密钥信封拼接客户端
 */
@Slf4j
public class EnvelopeClient {

    private final RestTemplate restTemplate;
    private final EleAiTenderInteractionProperties properties;
    private final InteractionRequestSigner signer;

    public EnvelopeClient(RestTemplate restTemplate,
                          EleAiTenderInteractionProperties properties,
                          InteractionRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.signer = signer;
    }

    public EnvelopeJoinResponse join(String authorization, EnvelopeJoinRequest request) {
        InteractionValidationUtils.validateEnvelopeJoinRequest(request);
        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<EnvelopeJoinRequest> entity = new HttpEntity<EnvelopeJoinRequest>(request, headers);
        ResponseEntity<InteractionResult<EnvelopeJoinResponse>> response = restTemplate.exchange(
                properties.resolveCryptoBaseUrl() + properties.getEnvelopeJoinPath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<EnvelopeJoinResponse>>() {
                });
        EnvelopeJoinResponse data = InteractionResultExtractor.extractData(response.getBody(), "信封拼接失败");
        log.info("INTERACTION LOCAL traceId={} api=crypto/envelope/join success=true",
                InteractionTraceSupport.getTraceId());
        return data;
    }
}
