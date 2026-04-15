package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 短信验证码实体
 */
@Data
@TableName("sup_sms_code")
@Schema(description = "短信验证码")
public class SupSmsCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "验证码")
    private String code;

    @Schema(description = "使用场景: LOGIN/REGISTER/RESET_PWD/BIND_PHONE")
    private String scene;

    @Schema(description = "状态: UNUSED/USED/EXPIRED")
    private String status;

    @Schema(description = "过期时间")
    private Date expireTime;

    @Schema(description = "发送IP地址")
    private String ipAddress;

    @Schema(description = "使用时间")
    private Date usedTime;

    @Schema(description = "创建时间")
    private Date createTime;
}
