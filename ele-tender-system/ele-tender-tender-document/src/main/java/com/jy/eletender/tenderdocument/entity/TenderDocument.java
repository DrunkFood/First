package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document")
public class TenderDocument extends BaseEntity {

    private Integer bizType;
    private String bizId;
    private String compileScope;
    private String projectId;
    private String tenderId;
    private Integer indexOf;
    private String projectCode;
    private String projectName;
    private String purchaseMethod;
    private String evalMethod;
    private String status;
    private String currentStepCode;
    private Integer versionNo;
    private Integer sourceVersionNo;
    private Date latestBasicInfoSyncTime;
    private Date latestBidRecordSyncTime;
}
