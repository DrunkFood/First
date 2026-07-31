package com.jy.eleaitender.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 手机验证码登录请求DTO
 */
@Data
@Schema(description = "手机验证码登录请求")
public class PhoneLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{4,6}$", message = "验证码格式不正确")
    @Schema(description = "验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "是否已同意用户服务协议和隐私政策")
    private Boolean agreementAccepted;

    @Schema(description = "已同意的协议类型")
    private List<String> acceptedAgreementTypes;

    @Schema(description = "协议版本")
    private String agreementVersion;
}
