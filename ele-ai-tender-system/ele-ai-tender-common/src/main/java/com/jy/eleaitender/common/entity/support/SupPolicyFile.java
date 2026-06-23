package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 政策文件实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_policy_file")
@Schema(description = "政策文件")
public class SupPolicyFile extends BaseEntity {

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件分类: LAW/REGULATION/POLICY")
    private String fileCategory;

    @Schema(description = "适用项目类别，多个用逗号分隔: SMALL_TRADE/GOVERNMENT_PROCUREMENT/COMPREHENSIVE_TRADE")
    private String applicableCategory;

    @Schema(description = "关联文件服务的file_id")
    private Long fileId;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件格式: PDF/DOCX/DOC/XLSX")
    private String fileType;

    @Schema(description = "文件描述")
    private String description;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
