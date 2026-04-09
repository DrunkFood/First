package com.jy.eletender.common.entity.support;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统访问日志实体
 */
@Data
@TableName("sup_access_log")
@Schema(description = "系统访问日志")
public class SysAccessLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "日志类型")
    private String logType;

    @Schema(description = "HTTP方法")
    private String httpMethod;

    @Schema(description = "请求URI")
    private String requestUri;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "响应状态码")
    private Integer statusCode;

    @Schema(description = "是否成功")
    private Integer successFlag;

    @Schema(description = "耗时毫秒")
    private Long elapsedMs;

    @Schema(description = "Token类型")
    private String tokenType;

    @Schema(description = "应用Key")
    private String appKey;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "企业ID")
    private String enterpriseId;

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "标段ID")
    private String tenderId;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "请求头摘要")
    private String requestHeaders;

    @Schema(description = "请求体摘要")
    private String requestBody;

    @Schema(description = "响应体摘要")
    private String responseBody;

    @Schema(description = "异常类型")
    private String errorType;

    @Schema(description = "异常信息")
    private String errorMessage;

    @Schema(description = "记录时间")
    private Date createTime;
}
