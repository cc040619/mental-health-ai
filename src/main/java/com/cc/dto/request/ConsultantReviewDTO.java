package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 咨询评价提交请求
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class ConsultantReviewDTO {

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer rating;

    @Size(max = 500, message = "评价内容不能超过500字")
    private String content;
}
