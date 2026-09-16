package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 危机词启停用 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CrisisWordStatusDTO {

    /** 0-停用 1-启用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态仅支持 0/1")
    @Max(value = 1, message = "状态仅支持 0/1")
    private Integer status;
}
