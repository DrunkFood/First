package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document_version")
public class TenderDocumentVersion extends BaseEntity {

    private Long tenderDocumentId;
    private Integer versionNo;
    private String projectId;
    private String status;
    private Date generateTime;
    private String summaryJson;
}
