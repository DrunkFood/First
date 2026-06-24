package com.jy.eleaitender.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewConfigTest {

    @Test
    void defaultConfig_shouldBeScoreMode() {
        ReviewConfig config = ReviewConfig.defaultConfig();
        assertThat(config.getScoreMode()).isEqualTo("SCORE");
    }

    @Test
    void fromJson_missingScoreMode_shouldFallbackToScore() {
        String json = """
                {"reviewTypes":[{"reviewType":"TECHNICAL","enabled":true,"generateStandard":true}]}
                """;
        ReviewConfig config = ReviewConfig.fromJson(json);
        assertThat(config.getScoreMode()).isEqualTo("SCORE");
        assertThat(config.isWeightMode()).isFalse();
    }

    @Test
    void fromJson_weightMode_shouldParse() {
        String json = """
                {"scoreMode":"WEIGHT","reviewTypes":[{"reviewType":"TECHNICAL","enabled":true,"generateStandard":true}]}
                """;
        ReviewConfig config = ReviewConfig.fromJson(json);
        assertThat(config.isWeightMode()).isTrue();
    }

    @Test
    void defaultConfig_技术标和资信标区分主客观_其他不区分() {
        ReviewConfig config = ReviewConfig.defaultConfig();
        assertFalse(config.isDistinguishSubjectivity("COMPLIANCE"));
        assertTrue(config.isDistinguishSubjectivity("TECHNICAL"));
        assertTrue(config.isDistinguishSubjectivity("CREDIT"));
        assertFalse(config.isDistinguishSubjectivity("COMMERCIAL"));
    }

    @Test
    void 老数据无字段时_回退到旧硬编码() {
        // 老数据 JSON 不含 distinguishSubjectivity 字段
        String oldJson = "{\"reviewTypes\":["
                + "{\"reviewType\":\"COMPLIANCE\",\"enabled\":true,\"generateStandard\":true},"
                + "{\"reviewType\":\"TECHNICAL\",\"enabled\":true,\"generateStandard\":true},"
                + "{\"reviewType\":\"CREDIT\",\"enabled\":true,\"generateStandard\":true},"
                + "{\"reviewType\":\"COMMERCIAL\",\"enabled\":true,\"generateStandard\":true}"
                + "]}";
        ReviewConfig config = ReviewConfig.fromJson(oldJson);
        assertFalse(config.isDistinguishSubjectivity("COMPLIANCE"));
        assertTrue(config.isDistinguishSubjectivity("TECHNICAL"));
        assertTrue(config.isDistinguishSubjectivity("CREDIT"));
        assertFalse(config.isDistinguishSubjectivity("COMMERCIAL"));
    }

    @Test
    void 显式关闭时_技术标不区分主客观() {
        String json = "{\"reviewTypes\":["
                + "{\"reviewType\":\"TECHNICAL\",\"enabled\":true,\"generateStandard\":true,\"distinguishSubjectivity\":false}"
                + "]}";
        ReviewConfig config = ReviewConfig.fromJson(json);
        assertFalse(config.isDistinguishSubjectivity("TECHNICAL"));
    }

    @Test
    void 显式开启时_商务标区分主客观() {
        String json = "{\"reviewTypes\":["
                + "{\"reviewType\":\"COMMERCIAL\",\"enabled\":true,\"generateStandard\":true,\"distinguishSubjectivity\":true}"
                + "]}";
        ReviewConfig config = ReviewConfig.fromJson(json);
        assertTrue(config.isDistinguishSubjectivity("COMMERCIAL"));
    }

    @Test
    void 未知类型返回false() {
        ReviewConfig config = ReviewConfig.defaultConfig();
        assertFalse(config.isDistinguishSubjectivity("UNKNOWN"));
    }
}
