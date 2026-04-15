package com.jy.eleaitender.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求DTO
 */
@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "RSA加密后的密码(Base64)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "密钥ID不能为空")
    @Schema(description = "RSA公钥ID，用于标识使用哪个密钥对解密", requiredMode = Schema.RequiredMode.REQUIRED)
    private String keyId;
}
