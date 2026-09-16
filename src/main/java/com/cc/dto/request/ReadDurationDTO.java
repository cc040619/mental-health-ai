package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文章阅读时长上报 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ReadDurationDTO {

    // 本次阅读时长（秒），范围 1..7200（2小时上限，防刷）
    @NotNull(message = "阅读时长不能为空")
    @Min(value = 1, message = "阅读时长不能小于1秒")
    @Max(value = 7200, message = "阅读时长不能超过7200秒")
    private Integer seconds;
}
