package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 高危预警干预 DTO
 * <p>状态机：0-未处理 → 1-已干预 → 2-已处理（可回退到已处理前一状态）。
 * 状态合法性由 Service 层状态机校验，DTO 层只做非空。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class InterventionDTO {

    @NotNull(message = "目标状态不能为空")
    private Integer status;

    @Size(max = 500, message = "干预备注不能超过500字")
    private String note;
}
