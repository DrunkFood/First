package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_rule_node")
public class TenderRuleNode extends BaseEntity {

    private Long tenderDocumentId;
    private Long ruleHeaderId;
    private String projectId;
    private String tenderId;
    private Long parentId;
    private Integer level;
    private Integer sortNo;
    private String itemContent;
    private BigDecimal scoreMin;
    private BigDecimal scoreMax;
    private String scoreStandard;
    private String scoreAttribute;
    private Integer leafFlag;
}
