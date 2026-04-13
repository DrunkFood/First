package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 信封拼接响应 — 还原后的 bidderPwdStr。
 */
@Data
public class EnvelopeJoinResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 还原后的 bidderPwdStr（base64 编码，32 字节） */
    private String bidderPwdStr;
}
