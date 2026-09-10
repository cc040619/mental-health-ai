package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开始测评请求 DTO
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentStartDTO {

    // 量表ID
    @NotNull(message = "量表ID不能为空")
    private Long scaleId;
}
