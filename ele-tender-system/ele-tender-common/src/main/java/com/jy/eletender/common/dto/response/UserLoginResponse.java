package com.jy.eletender.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户登录响应DTO
 */
@Data
@Schema(description = "用户登录响应")
public class UserLoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Token")
    private String token;

    @Schema(description = "过期时间（秒）")
    private Long expireIn;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Schema(description = "权限列表")
    private List<String> permissions;

    @Data
    @Schema(description = "用户信息")
    public static class UserInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "真实姓名")
        private String realName;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "角色列表")
        private List<String> roles;
    }
}
