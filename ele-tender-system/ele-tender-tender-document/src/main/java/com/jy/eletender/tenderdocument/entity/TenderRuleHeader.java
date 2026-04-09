package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_rule_header")
public class TenderRuleHeader extends BaseEntity {

    private Long tenderDocumentId;
    private String projectId;
    private String tenderId;
    private String tenderName;
    private String evalMethod;
    private String nodeCategory;
    private String reviewMode;
    private BigDecimal totalScore;
    private BigDecimal weightRate;
}
