package com.jy.eleaitender.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommaSeparatedFieldSqlTest {

    @Test
    void shouldMatchSingleAndCommaSeparatedValues() {
        assertThat(CommaSeparatedFieldSql.containsValue("GOVERNMENT_PROCUREMENT", "GOVERNMENT_PROCUREMENT")).isTrue();
        assertThat(CommaSeparatedFieldSql.containsValue(
                "SMALL_TRADE,GOVERNMENT_PROCUREMENT,COMPREHENSIVE_TRADE",
                "GOVERNMENT_PROCUREMENT")).isTrue();
        assertThat(CommaSeparatedFieldSql.containsValue(
                " SMALL_TRADE , GOVERNMENT_PROCUREMENT ",
                "GOVERNMENT_PROCUREMENT")).isTrue();
    }

    @Test
    void shouldNotMatchPartialTokensOrBlankValues() {
        assertThat(CommaSeparatedFieldSql.containsValue("GOVERNMENT_PROCUREMENT_EXTRA", "GOVERNMENT_PROCUREMENT")).isFalse();
        assertThat(CommaSeparatedFieldSql.containsValue("SMALL_TRADE", "GOVERNMENT_PROCUREMENT")).isFalse();
        assertThat(CommaSeparatedFieldSql.containsValue(null, "GOVERNMENT_PROCUREMENT")).isFalse();
        assertThat(CommaSeparatedFieldSql.containsValue("GOVERNMENT_PROCUREMENT", null)).isFalse();
        assertThat(CommaSeparatedFieldSql.containsValue("GOVERNMENT_PROCUREMENT", " ")).isFalse();
    }

    @Test
    void shouldBuildParameterizedSqlFragment() {
        assertThat(CommaSeparatedFieldSql.contains("applicable_category"))
                .isEqualTo("CONCAT(',', REPLACE(applicable_category, ' ', ''), ',') LIKE CONCAT('%,', {0}, ',%')");
    }
}
