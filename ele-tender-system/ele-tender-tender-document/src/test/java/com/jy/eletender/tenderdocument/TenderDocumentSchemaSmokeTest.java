package com.jy.eletender.tenderdocument;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TenderDocumentSchemaSmokeTest {

    @Test
    void shouldContainTenderDocumentTablesInInitScript() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/init.sql");
        assertThat(resource.exists()).isTrue();

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql)
                .contains("td_project_lock")
                .contains("td_tender_document")
                .contains("td_tender_document_step")
                .contains("td_tender_document_snapshot")
                .contains("td_tender_rule_header")
                .contains("td_tender_rule_score_config")
                .contains("td_tender_rule_node")
                .contains("td_tender_document_file")
                .contains("td_tender_document_version")
                .contains("td_tender_ca_keys_snapshot")
                .contains("td_tender_document_callback")
                .contains("td_tender_document_callback_counter")
                .contains("td_tender_document_generation_record");
    }

}
