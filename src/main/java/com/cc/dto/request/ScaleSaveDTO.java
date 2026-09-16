package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 量表基本信息保存 DTO（创建/编辑）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ScaleSaveDTO {

    @NotBlank(message = "量表编码不能为空")
    @Size(max = 50, message = "量表编码不能超过50字")
    private String code;

    @NotBlank(message = "量表名称不能为空")
    @Size(max = 100, message = "量表名称不能超过100字")
    private String name;

    @NotBlank(message = "类别不能为空")
    @Pattern(regexp = "EMOTION|PERSONALITY|STRESS|SLEEP|INTERPERSONAL",
            message = "类别必须为 EMOTION/PERSONALITY/STRESS/SLEEP/INTERPERSONAL 之一")
    private String category;

    private String description;

    @Size(max = 1000, message = "指导语不能超过1000字")
    private String guidance;
}
