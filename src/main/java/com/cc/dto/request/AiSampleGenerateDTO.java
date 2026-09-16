package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 发起质量抽检 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AiSampleGenerateDTO {

    @Min(value = 1, message = "抽检数量至少为1")
    @Max(value = 20, message = "抽检数量最多为20")
    private Integer count;
}
