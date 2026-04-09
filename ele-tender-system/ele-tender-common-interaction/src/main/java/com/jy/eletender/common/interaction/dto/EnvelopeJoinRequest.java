package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 信封拼接请求 — 将 CA 解密后的各层段拼接还原 bidderPwdStr。
 */
@Data
public class EnvelopeJoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<HashKeyItem> hashKeyList;

    @Data
    public static class HashKeyItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** CA 锁标识 */
        private String caId;

        /** CA 公钥加密后的段（base64） */
        private String hashKeyE;

        /** CA 私钥解密后的段（base64） */
        private String hashKeyD;

        /** 分段顺序 */
        private int order;
    }
}
