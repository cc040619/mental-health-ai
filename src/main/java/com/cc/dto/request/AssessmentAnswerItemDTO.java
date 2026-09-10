package com.cc.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 测评单题答案项 DTO
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentAnswerItemDTO {

    // 题目索引（从0开始）
    @NotNull(message = "题目索引不能为空")
    @Min(value = 0, message = "题目索引不能为负数")
    private Integer questionIndex;

    // 所选项的分值
    @NotNull(message = "选项分值不能为空")
    @Min(value = 0, message = "选项分值不能为负数")
    private Integer score;
}
