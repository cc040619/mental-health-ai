package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通用题目库题目保存 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class QuestionSaveDTO {

    @NotBlank(message = "题干不能为空")
    @Size(max = 500, message = "题干不能超过500字")
    private String stem;

    @NotBlank(message = "题型不能为空")
    @Pattern(regexp = "SINGLE|MULTIPLE|SCALE", message = "题型必须为 SINGLE/MULTIPLE/SCALE")
    private String type;

    // 选项 JSON 字符串：[{label, score}]，Service 层校验 ≥2 项且每项含 label/score
    @NotBlank(message = "选项不能为空")
    private String options;

    @Size(max = 50, message = "维度标签不能超过50字")
    private String dimensionTag;

    @Size(max = 30, message = "分类不能超过30字")
    private String category;
}
