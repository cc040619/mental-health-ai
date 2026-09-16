package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 课程上下架状态变更 DTO
 *
 * <p>状态机硬校验在 Service 层：仅 PUBLISHED(2) ↔ OFFLINE(4) 互转，
 * 非目标状态抛 BusinessException。DTO 层只做非空校验，避免 @Pattern 用在 Integer 上。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CourseStatusDTO {

    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
