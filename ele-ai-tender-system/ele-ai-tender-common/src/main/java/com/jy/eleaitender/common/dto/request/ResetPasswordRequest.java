package com.jy.eleaitender.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "短信验证码重置密码请求")
public class ResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{4,6}$", message = "验证码格式不正确")
    @Schema(description = "短信验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "密钥ID不能为空")
    @Schema(description = "RSA公钥ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String keyId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    @Schema(description = "RSA加密后的新密码(Base64)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
