package com.jy.eleaitender.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送验证码请求DTO
 */
@Data
@Schema(description = "发送验证码请求")
public class SendSmsCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "使用场景: LOGIN/RESET_PWD，默认LOGIN")
    private String scene = "LOGIN";
}
