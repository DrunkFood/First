package com.jy.eleaitender.common.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户政策文件实体
 * 对应表: ai_policy_file
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_policy_file")
@Schema(description = "用户政策文件")
public class AiPolicyFile extends BaseEntity {

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件分类: LAW/REGULATION/POLICY")
    private String fileCategory;

    @Schema(description = "适用项目类别")
    private String applicableCategory;

    @Schema(description = "关联file_info的id")
    private Long fileId;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件格式: PDF/DOCX/DOC/XLSX")
    private String fileType;

    @Schema(description = "文件描述")
    private String description;

    @Schema(description = "上传用户ID")
    private Long userId;

    @Schema(description = "状态: 0=禁用 1=启用")
    private Integer status;
}
