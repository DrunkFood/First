package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 接入系统实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_access_system")
@Schema(description = "接入系统")
public class SysAccessSystem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "系统URL")
    private String systemUrl;

    @Schema(description = "应用Key")
    private String appKey;

    @Schema(description = "应用密钥")
    private String appSecret;

    @Schema(description = "有效期截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "系统描述")
    private String description;

    @Schema(description = "招标文件后缀")
    private String tenderDocumentSuffix;

    @Schema(description = "投标文件后缀")
    private String bidDocumentSuffix;
}
