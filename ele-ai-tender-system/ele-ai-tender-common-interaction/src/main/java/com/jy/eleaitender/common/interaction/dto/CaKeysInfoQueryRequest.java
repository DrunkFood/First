package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 开标 CA 锁信息查询请求。
 */
@Data
public class CaKeysInfoQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String projectId;

    private String tenderId;
}
