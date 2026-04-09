package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document_step")
public class TenderDocumentStep extends BaseEntity {

    private Long tenderDocumentId;
    private String stepCode;
    private Integer stepOrder;
    private String stepStatus;
    private Date completeTime;
    private Date lastValidateTime;
    private String lastValidateResult;
}
