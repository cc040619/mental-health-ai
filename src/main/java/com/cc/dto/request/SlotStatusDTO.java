package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单个排班时段停用/恢复请求 DTO。
 * 仅允许 0-可约 / 2-停用 互转；已约(1)时段由 Service 拒绝，防止破坏已确认预约。
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class SlotStatusDTO {

    /** 0-恢复可约 2-停用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
