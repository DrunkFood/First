package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.TenderEntryContext;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenderDocumentEntryUrlBuilderTest {

    @Test
    void shouldBuildEntryUrlWithEncodedBusinessParameters() {
        EleTenderInteractionProperties properties = new EleTenderInteractionProperties();
        properties.setPageBaseUrl("http://localhost:8080");

        TenderDocumentEntryUrlBuilder builder = new TenderDocumentEntryUrlBuilder(properties);
        TenderEntryContext context = new TenderEntryContext();
        context.setBizType(1);
        context.setBizId("BIZ-1");
        context.setProjectId("P-1");
        context.setTenderId("T-1");
        context.setToken("token-123");

        String url = builder.build(context);

        assertThat(url).contains("bizType=1");
        assertThat(url).contains("bizId=BIZ-1");
        assertThat(url).contains("projectId=P-1");
        assertThat(url).contains("tenderId=T-1");
        assertThat(url).contains("token=token-123");
    }

    @Test
    void shouldBuildEntryUrlWhenPageBaseUrlContainsHashRoute() {
        EleTenderInteractionProperties properties = new EleTenderInteractionProperties();
        properties.setPageBaseUrl("http://10.11.22.121:81/#/create_tender_file");

        TenderDocumentEntryUrlBuilder builder = new TenderDocumentEntryUrlBuilder(properties);
        TenderEntryContext context = new TenderEntryContext();
        context.setBizType(1);
        context.setBizId("BIZ-1");
        context.setProjectId("P-1");
        context.setTenderId("T-1");
        context.setToken("token-123");

        String url = builder.build(context);

        assertThat(url).startsWith("http://10.11.22.121:81/#/create_tender_file/tender-document/compose?");
        assertThat(url).contains("bizType=1");
        assertThat(url).contains("bizId=BIZ-1");
        assertThat(url).contains("projectId=P-1");
        assertThat(url).contains("tenderId=T-1");
        assertThat(url).contains("token=token-123");
    }

    @Test
    void shouldNotAppendDuplicatePagePathWhenBaseAlreadyContainsComposePath() {
        EleTenderInteractionProperties properties = new EleTenderInteractionProperties();
        properties.setPageBaseUrl("http://10.11.22.121:81/#/create_tender_file/tender-document/compose");

        TenderDocumentEntryUrlBuilder builder = new TenderDocumentEntryUrlBuilder(properties);
        TenderEntryContext context = new TenderEntryContext();
        context.setBizType(1);
        context.setBizId("BIZ-1");
        context.setProjectId("P-1");
        context.setTenderId("T-1");
        context.setToken("token-123");

        String url = builder.build(context);

        assertThat(url).startsWith("http://10.11.22.121:81/#/create_tender_file/tender-document/compose?");
        assertThat(url).doesNotContain("/tender-document/compose/tender-document/compose");
    }
}
