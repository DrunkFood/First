package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.util.List;

/**
 * 政策文件响应VO
 */
@Data
public class PolicyFileQueryResponse {

    /**
     * 政策文件列表
     */
    List<InteractionPolicyFileVO> policyFileList;

}
