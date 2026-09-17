package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 首次设置密码请求 DTO（仅无密码用户可用）
 *
 * <p>密码规则：大于 8 位（至少 9 位），且包含字母、数字和特殊符号。</p>
 *
 * @author cc
 * @date 2026-09-16
 */
@Data
public class SetPasswordDTO {

    /** 密码正则：至少 9 位，含字母、数字、特殊符号 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{9,}$";

    @NotBlank(message = "密码不能为空")
    @Size(min = 9, max = 50, message = "密码长度必须在9到50个字符之间")
    @Pattern(regexp = PASSWORD_REGEX, message = "密码需大于8位，且包含字母、数字和特殊符号")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
