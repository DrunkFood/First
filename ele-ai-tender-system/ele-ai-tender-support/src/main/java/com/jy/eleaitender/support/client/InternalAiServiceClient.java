package com.jy.eleaitender.support.client;

import com.jy.eleaitender.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * 内部AI服务客户端
 * 用于support模块通知AI模块刷新线程池参数
 */
@Slf4j
public class InternalAiServiceClient {

    private static final String SERVICE_NAME = "ele-ai-tender-support";

    private final RestTemplate restTemplate;
    private final InternalAiServiceProperties properties;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    public InternalAiServiceClient(RestTemplate restTemplate, InternalAiServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 通知AI模块刷新线程池参数
     */
    public boolean notifyRefreshThreadPool() {
        String url = properties.getBaseUrl() + "/actuator/threadpool/refresh";
        log.info("通知AI模块刷新线程池参数: url={}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAuthHeader(headers);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            log.info("AI模块线程池刷新响应: status={}", response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("通知AI模块刷新线程池参数失败: {}", e.getMessage());
            return false;
        }
    }

    private void addAuthHeader(HttpHeaders headers) {
        String token = getOrCreateToken();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private synchronized String getOrCreateToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && tokenExpireAt > now + 60_000) {
            return cachedToken;
        }
        String secret = properties.getJwtSecret();
        cachedToken = JwtUtil.generateServiceToken(SERVICE_NAME, secret, properties.getTokenExpiration());
        tokenExpireAt = now + properties.getTokenExpiration();
        return cachedToken;
    }
}
