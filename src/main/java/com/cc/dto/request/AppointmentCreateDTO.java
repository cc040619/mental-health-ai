package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建预约请求
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class AppointmentCreateDTO {

    @NotNull(message = "请选择咨询师")
    private Long consultantId;

    @NotNull(message = "请选择预约时段")
    private Long slotId;

    @Size(max = 500, message = "问题描述不能超过500字")
    private String problemDesc;
}
