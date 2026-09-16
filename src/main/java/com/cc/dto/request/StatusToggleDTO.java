package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用状态启停请求 DTO（0/1）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class StatusToggleDTO {

    // 0-停用 1-启用
    @NotNull(message = "状态不能为空")
    private Integer status;
}
