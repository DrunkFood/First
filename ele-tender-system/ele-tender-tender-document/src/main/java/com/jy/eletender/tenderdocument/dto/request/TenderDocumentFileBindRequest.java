package com.jy.eletender.tenderdocument.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenderDocumentFileBindRequest {

    /**
     * 邀请类项目按标段绑定文件时必填；公开类项目由后端统一按项目级处理。
     */
    private String tenderId;

    @NotNull
    private Long fileId;

    @NotBlank
    private String fileName;

    @NotNull
    private Long fileSize;

    private String contentType;

    /**
     * 文件服务返回的 SHA-256，编制系统仅做记录与展示，不在绑定时重复校验文件内容。
     */
    private String fileSha256;
}
