package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 测评进度保存 DTO（中途退出续答）
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentProgressDTO {

    // 当前答到第几题（从0开始）
    @NotNull(message = "当前题序不能为空")
    @Min(value = 0, message = "当前题序不能为负数")
    private Integer currentIndex;

    // 已作答答案列表
    @Valid
    @NotNull(message = "答案列表不能为空")
    private List<AssessmentAnswerItemDTO> answers;
}
