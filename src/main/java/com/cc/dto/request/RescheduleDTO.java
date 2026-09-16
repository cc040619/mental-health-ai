package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 后台预约改期请求：原子抢占新时段，禁止先查后改
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class RescheduleDTO {

    @NotNull(message = "新时段不能为空")
    private Long slotId;
}
