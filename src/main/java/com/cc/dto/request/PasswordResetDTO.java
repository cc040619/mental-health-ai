package com.cc.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码重置请求 DTO（免登录，凭邮箱验证码重置）
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
public class PasswordResetDTO {

    // 邮箱
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    // 邮箱验证码
    @NotBlank(message = "验证码不能为空")
    private String code;

    // 新密码
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
    private String newPassword;
}
