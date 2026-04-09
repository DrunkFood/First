package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document_callback")
public class TenderDocumentCallback extends BaseEntity {

    private Long tenderDocumentId;
    private Integer versionNo;
    private String projectId;
    private String tenderId;
    private String fileRole;
    private Long fileId;
    private String callbackStatus;
    private Date callbackTime;
    private String responseCode;
    private String responseMessage;
    private Integer retryCount;
    private String operatorId;
    private String operatorName;
}
