package com.jy.eleaitender.interaction.core.support;

import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * RestTemplate 工厂
 */
public final class InteractionRestTemplateFactory {

    private InteractionRestTemplateFactory() {
    }

    public static RestTemplate create(EleAiTenderInteractionProperties properties, ClientHttpRequestInterceptor interceptor) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        stripXmlMessageConverters(restTemplate);
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.add(interceptor);
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    /**
     * starter 出站仅允许 JSON（文件下载除外由 byte[] 处理），避免宿主 classpath 中的 XML 转换器改变协议。
     */
    private static void stripXmlMessageConverters(RestTemplate restTemplate) {
        restTemplate.getMessageConverters().removeIf(InteractionRestTemplateFactory::supportsXml);
    }

    private static boolean supportsXml(HttpMessageConverter<?> converter) {
        for (MediaType mediaType : converter.getSupportedMediaTypes()) {
            if (MediaType.APPLICATION_XML.includes(mediaType) || MediaType.TEXT_XML.includes(mediaType)) {
                return true;
            }
            String subtype = mediaType.getSubtype();
            if (subtype != null && subtype.toLowerCase().endsWith("+xml")) {
                return true;
            }
        }
        return false;
    }
}
