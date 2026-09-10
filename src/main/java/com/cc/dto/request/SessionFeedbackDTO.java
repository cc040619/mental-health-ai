package com.cc.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 会话评价反馈 DTO
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class SessionFeedbackDTO {

    // 评分 1-5 星
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer rating;

    // 文字反馈（选填）
    @Size(max = 500, message = "反馈内容最多500字")
    private String feedback;
}
