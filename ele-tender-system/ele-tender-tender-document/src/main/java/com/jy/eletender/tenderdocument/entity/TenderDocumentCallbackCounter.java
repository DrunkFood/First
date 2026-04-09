package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document_callback_counter")
public class TenderDocumentCallbackCounter extends BaseEntity {

    private Long tenderDocumentId;
    private Integer versionNo;
    private String fileRole;
    private String tenderId;
    private Integer currentRetryCount;
}
