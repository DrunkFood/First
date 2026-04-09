package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document_snapshot")
public class TenderDocumentSnapshot extends BaseEntity {

    private Long tenderDocumentId;
    private String stepCode;
    private String snapshotJson;
    private Date sourceSyncTime;
    private String sourceHash;
}
