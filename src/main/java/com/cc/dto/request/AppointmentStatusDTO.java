package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台预约状态变更请求：CONFIRMED/CANCELED/COMPLETED
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class AppointmentStatusDTO {

    @NotBlank(message = "目标状态不能为空")
    private String status;

    /** 取消原因（仅 status=CANCELED 时使用） */
    @Size(max = 500, message = "取消原因不能超过500字")
    private String cancelReason;
}
