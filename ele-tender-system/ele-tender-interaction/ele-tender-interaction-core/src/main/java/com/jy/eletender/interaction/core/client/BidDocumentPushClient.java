package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushResponse;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.util.InteractionResultExtractor;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
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
 * 投标文件预存客户端
 */
@Slf4j
public class BidDocumentPushClient {

    private final RestTemplate restTemplate;
    private final EleTenderInteractionProperties properties;
    private final InteractionRequestSigner signer;

    public BidDocumentPushClient(RestTemplate restTemplate,
                             EleTenderInteractionProperties properties,
                             InteractionRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.signer = signer;
    }

    public BidDocumentPushResponse push(String authorization, BidDocumentPushRequest request) {
        InteractionValidationUtils.validateBidDocumentPushRequest(request);
        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<BidDocumentPushRequest> entity = new HttpEntity<BidDocumentPushRequest>(request, headers);
        ResponseEntity<InteractionResult<BidDocumentPushResponse>> response = restTemplate.exchange(
                properties.resolveCryptoBaseUrl() + properties.getBidDocumentPushPath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<BidDocumentPushResponse>>() {
                });
        BidDocumentPushResponse data = InteractionResultExtractor.extractData(response.getBody(), "投标文件预存失败");
        log.info("INTERACTION LOCAL traceId={} api=crypto/bid-document/push success=true fileId={} uploadResult={}",
                InteractionTraceSupport.getTraceId(),
                data.getFileId(),
                data.getUploadResult());
        return data;
    }
}
