package com.jy.eleaitender.support.service;

import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.config.SmsGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公司短信网关客户端。
 */
@Slf4j
@Component
public class SmsGatewayClient {

    private static final String SUCCESS_RESULT = "0";
    private static final String SEND_FAILED_MESSAGE = "验证码发送失败，请稍后重试";

    private final SmsGatewayProperties properties;
    private final RestOperations restOperations;

    public SmsGatewayClient(
            SmsGatewayProperties properties,
            @Qualifier("smsGatewayRestTemplate") RestOperations restOperations) {
        this.properties = properties;
        this.restOperations = restOperations;
    }

    public void sendCode(String phone, String code, String scene) {
        String content = buildContent(code, scene);
        if (!Boolean.TRUE.equals(properties.getIsformal())) {
            log.info("短信非正式模式，跳过网关发送，phone={}, scene={}", phone, scene);
            return;
        }

        validateFormalConfig();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", properties.getUsername());
        form.add("password", properties.getPwd());
        form.add("mobile", phone);
        form.add("content", content);
        form.add("extend", properties.getExtend());
        form.add("level", "1");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        String response;
        try {
            response = restOperations.postForObject(properties.getUrl(), entity, String.class);
        } catch (RestClientException e) {
            log.warn("短信网关调用异常，phone={}, scene={}, error={}", phone, scene, e.getMessage());
            throw new BusinessException(SEND_FAILED_MESSAGE, e);
        }

        Map<String, String> responseMap = parseGatewayResponse(response);
        String result = responseMap.get("result");
        if (!SUCCESS_RESULT.equals(result)) {
            log.warn("短信网关返回失败，phone={}, scene={}, result={}, msg={}",
                    phone, scene, result, responseMap.get("msg"));
            throw new BusinessException(SEND_FAILED_MESSAGE);
        }

        log.info("短信网关发送成功，phone={}, scene={}", phone, scene);
    }

    static Map<String, String> parseGatewayResponse(String response) {
        if (StringUtils.isBlank(response)) {
            throw new BusinessException(SEND_FAILED_MESSAGE);
        }

        Map<String, String> result = new LinkedHashMap<>();
        String[] pairs = response.split("&");
        for (String pair : pairs) {
            int splitIndex = pair.indexOf('=');
            if (splitIndex <= 0) {
                continue;
            }
            String key = decode(pair.substring(0, splitIndex));
            String value = decode(pair.substring(splitIndex + 1));
            result.put(key, value);
        }

        if (!result.containsKey("result")) {
            throw new BusinessException(SEND_FAILED_MESSAGE);
        }
        return result;
    }

    private void validateFormalConfig() {
        if (StringUtils.isAnyBlank(
                properties.getUsername(),
                properties.getPwd(),
                properties.getExtend(),
                properties.getUrl())) {
            log.warn("短信网关正式发送配置不完整");
            throw new BusinessException(SEND_FAILED_MESSAGE);
        }
    }

    private static String buildContent(String code, String scene) {
        if ("RESET_PWD".equals(scene)) {
            return "您正在重置密码，验证码是：" + code + "，5分钟内有效，请勿泄露。";
        }
        return "您的验证码是：" + code + "，5分钟内有效，请勿泄露。";
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
