package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程审核 DTO
 *
 * <p>approve=true 表示通过，false 表示驳回（驳回必填原因）。
 * 状态机硬校验：仅 PENDING(待审核) 可走此接口，非目标状态直接抛 BusinessException。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CourseReviewDTO {

    @NotNull(message = "审核结果不能为空")
    private Boolean approve;

    @Size(max = 500, message = "驳回原因不能超过500字")
    private String rejectReason;
}
