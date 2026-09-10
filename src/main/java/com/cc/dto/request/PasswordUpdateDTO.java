package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO（登录态下使用，需校验旧密码）
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
public class PasswordUpdateDTO {

    // 原密码
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    // 新密码
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
    private String newPassword;
}
