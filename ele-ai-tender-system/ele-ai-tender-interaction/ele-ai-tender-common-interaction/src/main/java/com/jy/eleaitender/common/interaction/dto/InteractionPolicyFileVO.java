package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

/**
 * 政策文件响应VO
 */
@Data
public class InteractionPolicyFileVO {

    /**
     * 文件ID
     */
    private Long id;

    /**
     * 政策文件分类
     */
    private String fileCategory;

    /**
     * 适用项目类别
     */
    private String applicableCategory;

    /**
     * 关联file_info的id
     */
    private Long fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件格式
     */
    private String fileType;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 来源: SYSTEM/USER
     */
    private String source;

}
