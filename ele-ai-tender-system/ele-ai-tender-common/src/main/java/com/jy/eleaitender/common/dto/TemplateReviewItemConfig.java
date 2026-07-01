package com.jy.eleaitender.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Template-level manual review item stored inside reviewConfig JSON.
 */
@Data
public class TemplateReviewItemConfig {

    @JsonAlias("name")
    private String itemName;

    @JsonAlias("content")
    private String itemContent;

    private Integer sortOrder;

    private BigDecimal score;

    private BigDecimal weight;

    private String subjectivity;

    private Integer isRequired;

    private List<TemplateReviewItemConfig> children;
}
