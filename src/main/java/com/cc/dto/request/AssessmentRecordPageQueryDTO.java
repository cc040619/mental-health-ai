package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 测评历史分页查询 DTO
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentRecordPageQueryDTO {

    // 页码
    @Min(value = 1, message = "页码最小为1")
    private Long current = 1L;

    // 每页条数（上限50）
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 50, message = "每页条数最大为50")
    private Long size = 10L;

    // 量表ID筛选（可空）
    private Long scaleId;
}
