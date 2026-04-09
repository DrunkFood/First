package com.jy.eletender.tenderdocument.support;

import lombok.Data;

@Data
public class TenderDocumentGeneratedFile {

    private String tenderId;

    /**
     * 生成产物的粒度枚举名，取值见 TenderDocumentScopeType。
     */
    private String scopeType;

    /**
     * 生成产物角色枚举名，取值见 TenderDocumentFileType。
     */
    private String fileRole;
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String fileSha256;
}
