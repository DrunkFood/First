package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_ca_keys_snapshot")
public class TenderDocumentCaKeysSnapshot extends BaseEntity {

    private Long tenderDocumentId;
    private Integer versionNo;
    private String projectId;
    private String tenderId;
    private String sourceAppKey;
    private String traceId;
    private Date capturedTime;
    private Integer caKeysCount;
    private String caKeysJson;
}
