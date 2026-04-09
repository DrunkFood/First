package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class TenderDocumentCheckItemView {

    private String itemCode;
    private String itemName;
    private Boolean passed;
    private String message;

    /**
     * 每一个String, 代表这个节点失败的原因
     */
    private List<String> detail;
}
