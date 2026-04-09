package com.jy.eletender.tenderdocument.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("td_tender_document_file")
public class TenderDocumentFile extends BaseEntity {

    private Long tenderDocumentId;
    private String projectId;
    private String tenderId;
    private String scopeType;
    private String fileRole;
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String fileSha256;
    private Integer activeFlag;
    private Date createdTime;
}
