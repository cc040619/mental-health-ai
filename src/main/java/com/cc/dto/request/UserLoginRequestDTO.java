package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求 DTO
 *
 * @author cc
 * @date 2026-09-01
 */
@Data
public class UserLoginRequestDTO {
    @NotBlank(message = "用户名或邮箱不能为空")
    @Size(max = 100, message = "用户名或邮箱长度不能超过100个字符")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(max = 50, min = 6, message = "密码长度必须在6到50个字符之间")
    private String password;
}
