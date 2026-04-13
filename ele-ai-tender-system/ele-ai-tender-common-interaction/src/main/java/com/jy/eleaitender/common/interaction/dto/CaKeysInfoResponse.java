package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 开标 CA 锁信息查询响应。
 */
@Data
public class CaKeysInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务系统应返回“开标时有效”的 CA 锁信息集合。
     */
    private List<CaKeyInfo> caKeysInfo = new ArrayList<CaKeyInfo>();

    @Data
    public static class CaKeyInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer encryptOrder;

        private String userId;

        private String caId;

        private String caNo;

        private String publicKey;
    }
}
