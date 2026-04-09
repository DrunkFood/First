package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document_generation_record")
public class TenderDocumentGenerationRecord extends BaseEntity {

    private Long tenderDocumentId;
    private Integer versionNo;
    private String projectId;
    private String tenderId;
    private String generateStatus;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private String errorCode;
    private String errorMessage;
    private String traceId;
    private String operatorId;
    private String operatorName;
}
