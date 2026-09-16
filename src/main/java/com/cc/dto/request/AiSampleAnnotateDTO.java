package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 抽检标注 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AiSampleAnnotateDTO {

    /** 1-好 2-一般 3-坏 */
    @NotNull(message = "标注状态不能为空")
    @Min(value = 1, message = "标注状态非法")
    @Max(value = 3, message = "标注状态非法")
    private Integer status;

    @Min(value = 1, message = "评分为1-5")
    @Max(value = 5, message = "评分为1-5")
    private Integer score;

    @Size(max = 500, message = "备注长度不能超过500")
    private String note;
}
