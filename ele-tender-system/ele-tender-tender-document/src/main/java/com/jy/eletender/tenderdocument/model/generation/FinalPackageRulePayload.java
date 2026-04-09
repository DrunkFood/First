package com.jy.eletender.tenderdocument.model.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalPackageRulePayload {

    private Info info;

    @JsonProperty("QUALIFICATION")
    private RuleSection qualification;

    @JsonProperty("CONFORMITY")
    private RuleSection conformity;

    @JsonProperty("DETAIL")
    private RuleSection detail;

    @JsonProperty("CREDIT")
    private ScoreRuleSection credit;

    @JsonProperty("TECHNICAL")
    private ScoreRuleSection technical;

    @JsonProperty("BUSINESS")
    private ScoreRuleSection business;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Info {
        private String bidEvalMethod;
        private String reviewMode;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RuleSection {
        private String reviewMode;
        private List<RuleNode> scoreRules;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScoreRuleSection extends RuleSection {
        private Integer totalScore;
        private Integer percentage;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RuleNode {
        private String id;
        private Integer order;
        private String key;
        private String name;
        private BigDecimal score;
        private Integer pages;
        private BigDecimal lowest;
        private BigDecimal highest;
        private String standard;
        private String objectiveType;
        private List<RuleNode> children;
        private Boolean isPass;
        private Boolean isParent;
    }
}
