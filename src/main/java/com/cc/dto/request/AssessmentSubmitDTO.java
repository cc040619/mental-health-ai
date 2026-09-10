package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 测评提交作答 DTO
 *
 * @author cc
 * @date 2026-09-08
 */
@Data
public class AssessmentSubmitDTO {

    // 全部答案列表
    @Valid
    @NotEmpty(message = "答案不能为空")
    private List<AssessmentAnswerItemDTO> answers;
}
