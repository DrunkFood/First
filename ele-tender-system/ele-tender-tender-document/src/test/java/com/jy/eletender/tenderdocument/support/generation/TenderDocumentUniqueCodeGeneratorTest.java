package com.jy.eletender.tenderdocument.support.generation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenderDocumentUniqueCodeGeneratorTest {

    private final TenderDocumentUniqueCodeGenerator generator = new TenderDocumentUniqueCodeGenerator();

    @Test
    void shouldGenerateNonEmptySystemUniqueCode() {
        String code = generator.generate();

        assertThat(code).isNotBlank();
        assertThat(code).doesNotContain("-");
        assertThat(code).hasSize(32);
    }

    @Test
    void shouldGenerateDifferentUniqueCodesAcrossCalls() {
        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).isNotEqualTo(second);
    }
}
