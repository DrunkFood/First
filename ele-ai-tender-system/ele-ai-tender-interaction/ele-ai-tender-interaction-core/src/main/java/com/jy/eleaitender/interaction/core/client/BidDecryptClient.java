package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.BidDecryptStatusResponse;
import com.jy.eleaitender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eleaitender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.enums.InteractionResponseCode;
import com.jy.eleaitender.common.interaction.exception.InteractionException;
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
 * 投标文件解密客户端
 */
@Slf4j
public class BidDecryptClient {

    private final RestTemplate restTemplate;
    private final EleAiTenderInteractionProperties properties;
    private final InteractionRequestSigner signer;

    public BidDecryptClient(RestTemplate restTemplate,
                            EleAiTenderInteractionProperties properties,
                            InteractionRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.signer = signer;
    }

    public BidDecryptSubmitResponse submit(String authorization, BidDecryptSubmitRequest request) {
        InteractionValidationUtils.validateBidDecryptSubmitRequest(request);
        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<BidDecryptSubmitRequest> entity = new HttpEntity<BidDecryptSubmitRequest>(request, headers);
        ResponseEntity<InteractionResult<BidDecryptSubmitResponse>> response = restTemplate.exchange(
                properties.resolveCryptoBaseUrl() + properties.getBidDecryptSubmitPath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<BidDecryptSubmitResponse>>() {
                });
        BidDecryptSubmitResponse data = InteractionResultExtractor.extractData(response.getBody(), "提交解密请求失败");
        log.info("INTERACTION LOCAL traceId={} api=crypto/bid-decrypt/submit success=true recordId={}",
                InteractionTraceSupport.getTraceId(),
                data.getRecordId());
        return data;
    }

    public BidDecryptStatusResponse queryStatus(String authorization, String recordId) {
        requireText(recordId, "recordId不能为空");
        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<Void>(headers);
        ResponseEntity<InteractionResult<BidDecryptStatusResponse>> response = restTemplate.exchange(
                properties.resolveCryptoBaseUrl() + properties.getBidDecryptStatusPath().replace("{recordId}", recordId),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<InteractionResult<BidDecryptStatusResponse>>() {
                });
        BidDecryptStatusResponse data = InteractionResultExtractor.extractData(response.getBody(), "查询解密状态失败");
        log.info("INTERACTION LOCAL traceId={} api=crypto/bid-decrypt/status success=true recordId={} status={}",
                InteractionTraceSupport.getTraceId(),
                data.getRecordId(),
                data.getStatus());
        return data;
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, message);
        }
    }
}
