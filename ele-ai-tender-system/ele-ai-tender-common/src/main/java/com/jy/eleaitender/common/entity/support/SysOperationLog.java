package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志实体
 */
@Data
@TableName("sup_operation_log")
@Schema(description = "操作日志")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "操作用户名")
    private String userName;

    @Schema(description = "操作类型")
    private String operation;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求参数")
    private String params;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "执行时长(毫秒)")
    private Long executeTime;

    @Schema(description = "操作时间")
    private Date createTime;
}
