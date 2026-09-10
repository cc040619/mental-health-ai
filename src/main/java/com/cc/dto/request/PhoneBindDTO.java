package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 绑定/换绑手机号请求 DTO（登录态，凭当前用户邮箱验证码验证身份）
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
public class PhoneBindDTO {

    // 当前用户邮箱收到的验证码
    @NotBlank(message = "验证码不能为空")
    private String code;

    // 新手机号
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
