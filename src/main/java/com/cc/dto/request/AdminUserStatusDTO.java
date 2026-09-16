package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端用户状态变更参数：0 禁用 / 1 启用
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminUserStatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;
}
