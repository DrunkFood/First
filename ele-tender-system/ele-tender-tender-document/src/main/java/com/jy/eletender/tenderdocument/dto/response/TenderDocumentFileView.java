package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class TenderDocumentFileView {

    private String tenderId;
    private String tenderName;

    /**
     * 文件粒度枚举名，取值见 TenderDocumentScopeType。
     */
    private String scopeType;

    /**
     * 文件角色枚举名，取值见 TenderDocumentFileType。
     */
    private String fileRole;
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String fileSha256;
    private Date createTime;
}
