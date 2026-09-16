package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 咨询师取消预约请求 DTO。取消原因必填，展示给来访者；≤500 字。
 *
 * @author cc
 * @date 2026-09-16
 */
@Data
public class AppointmentCancelDTO {

    /** 取消原因（必填，来访者可见） */
    @NotBlank(message = "请填写取消原因")
    @Size(max = 500, message = "取消原因不能超过500字")
    private String reason;
}
