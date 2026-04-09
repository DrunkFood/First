package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_rule_score_config")
public class TenderRuleScoreConfig extends BaseEntity {

    private Long tenderDocumentId;
    private String scoreType;
}
