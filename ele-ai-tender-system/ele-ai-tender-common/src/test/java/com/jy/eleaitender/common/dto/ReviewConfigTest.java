package com.jy.eleaitender.common.dto;

import com.jy.eleaitender.common.enums.ScoreMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewConfigTest {

    @Test
    void defaultConfig_shouldBeScoreMode() {
        ReviewConfig config = ReviewConfig.defaultConfig();
        assertThat(config.getScoreModeOrDefault()).isEqualTo(ScoreMode.SCORE);
        assertThat(config.isWeightMode()).isFalse();
    }

    @Test
    void fromJson_missingScoreMode_shouldFallbackToScore() {
        String json = """
                {"reviewTypes":[{"reviewType":"TECHNICAL","enabled":true,"generateStandard":true}]}
                """;
        ReviewConfig config = ReviewConfig.fromJson(json);
        assertThat(config.isWeightMode()).isFalse();
        assertThat(config.getScoreModeOrDefault()).isEqualTo(ScoreMode.SCORE);
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
    void fromJson_invalidScoreMode_shouldFallbackToScore() {
        String json = """
                {"scoreMode":"INVALID","reviewTypes":[{"reviewType":"TECHNICAL","enabled":true,"generateStandard":true}]}
                """;
        ReviewConfig config = ReviewConfig.fromJson(json);
        assertThat(config.isWeightMode()).isFalse();
        assertThat(config.getScoreModeOrDefault()).isEqualTo(ScoreMode.SCORE);
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

    @Test
    void fromJson_含派生属性污染字段_应正常解析不丢失scoreMode() {
        // T1 期间 @JsonIgnore 修复前，getEnabledTypes/getScoreModeOrDefault/isWeightMode
        // 被 Jackson 当 getter 序列化出 enabledTypes/scoreModeOrDefault/weightMode 垃圾字段。
        // 旧数据可能含这些字段，fromJson 须容忍（FAIL_ON_UNKNOWN_PROPERTIES=false），
        // 且仍按 scoreMode 字段正确判别模式，不回退 defaultConfig 丢失权重配置。
        String pollutedJson = """
                {"scoreMode":"WEIGHT",
                 "enabledTypes":[],
                 "scoreModeOrDefault":"SCORE",
                 "weightMode":true,
                 "reviewTypes":[{"reviewType":"TECHNICAL","enabled":true,"generateStandard":true}]}
                """;
        ReviewConfig config = ReviewConfig.fromJson(pollutedJson);
        assertThat(config.isWeightMode()).isTrue();
        assertThat(config.getScoreModeOrDefault()).isEqualTo(ScoreMode.WEIGHT);
    }

    @Test
    void fromJson_shouldParseManualItems() {
        String json = """
                {"scoreMode":"SCORE","reviewTypes":[
                  {"reviewType":"COMPLIANCE","enabled":true,"generateStandard":false,
                   "manualItems":[{"itemName":"Manual Item","itemContent":"Manual Standard","score":10,
                     "children":[{"itemName":"Manual Child","itemContent":"Child Standard","score":5}]}]}
                ]}
                """;

        ReviewConfig config = ReviewConfig.fromJson(json);

        ReviewTypeConfig typeConfig = config.getEnabledTypes().get(0);
        assertThat(typeConfig.getManualItems()).hasSize(1);
        assertThat(typeConfig.getManualItems().get(0).getItemName()).isEqualTo("Manual Item");
        assertThat(typeConfig.getManualItems().get(0).getChildren()).hasSize(1);
        assertThat(typeConfig.getManualItems().get(0).getChildren().get(0).getScore())
                .isEqualByComparingTo("5");
    }
}
