package com.jy.eletender.tenderdocument.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenderDocumentEntryRequest {

    /**
     * 交互业务类型，沿用交互层约定：1=立项，2=答疑。
     */
    @NotNull
    private Integer bizType;

    @NotBlank
    private String bizId;

    @NotBlank
    private String projectId;

    /**
     * 入口统一要求传入 tenderId。
     * 公开类项目最终会由后端忽略该值，邀请类项目则用它定位标段级编制单。
     */
    @NotBlank
    private String tenderId;
}
