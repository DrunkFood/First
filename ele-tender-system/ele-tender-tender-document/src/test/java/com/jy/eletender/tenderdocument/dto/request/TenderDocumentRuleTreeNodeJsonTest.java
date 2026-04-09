package com.jy.eletender.tenderdocument.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenderDocumentRuleTreeNodeJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeObjectiveTypeFieldName() throws Exception {
        TenderDocumentRuleTreeNode node = new TenderDocumentRuleTreeNode();
        node.setObjectiveType("SUBJECTIVE");

        String json = objectMapper.writeValueAsString(node);

        assertThat(json).contains("\"objectiveType\":\"SUBJECTIVE\"");
        assertThat(json).doesNotContain("\"jectiveType\":");
    }

    @Test
    void shouldDeserializeObjectiveTypeFieldName() throws Exception {
        String json = """
                {
                  "objectiveType": "OBJECTIVE"
                }
                """;

        TenderDocumentRuleTreeNode node = objectMapper.readValue(json, TenderDocumentRuleTreeNode.class);

        assertThat(node.getObjectiveType()).isEqualTo("OBJECTIVE");
    }

    @Test
    void shouldRejectLegacyJectiveTypeFieldName() {
        String json = """
                {
                  "jectiveType": "SUBJECTIVE"
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, TenderDocumentRuleTreeNode.class))
                .hasMessageContaining("jectiveType");
    }
}
