package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 标录方案响应
 */
@Data
public class BidRecordSchemeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标录方案（JSON 数组字符串）
     */
    private String schemeContent;
}
