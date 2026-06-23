package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 政策文件响应VO
 */
@Data
@Schema(description = "政策文件")
public class PolicyFileVO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件分类")
    private String fileCategory;

    @Schema(description = "适用项目类别，多个用逗号分隔")
    private String applicableCategory;

    @Schema(description = "关联file_info的id")
    private Long fileId;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件格式")
    private String fileType;

    @Schema(description = "文件描述")
    private String description;

    @Schema(description = "状态: 0=禁用 1=启用")
    private Integer status;

    @Schema(description = "来源: SYSTEM/USER")
    private String source;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "创建人")
    private String createName;
}
