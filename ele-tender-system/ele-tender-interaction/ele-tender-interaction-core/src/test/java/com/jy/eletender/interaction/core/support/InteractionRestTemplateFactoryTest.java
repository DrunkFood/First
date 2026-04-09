package com.jy.eletender.interaction.core.support;

import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionRestTemplateFactoryTest {

    @Test
    void shouldStripXmlMessageConverters() {
        EleTenderInteractionProperties properties = new EleTenderInteractionProperties();
        RestTemplate restTemplate = InteractionRestTemplateFactory.create(properties,
                (request, body, execution) -> execution.execute(request, body));

        boolean hasXmlConverter = restTemplate.getMessageConverters().stream()
                .anyMatch(InteractionRestTemplateFactoryTest::supportsXml);

        assertThat(hasXmlConverter).isFalse();
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
