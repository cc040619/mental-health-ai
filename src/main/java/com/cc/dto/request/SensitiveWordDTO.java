package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 敏感词新增/编辑请求
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class SensitiveWordDTO {

    // 编辑时必传，新增为 null
    private Long id;

    @NotBlank(message = "敏感词不能为空")
    @Size(max = 50, message = "敏感词不能超过50字")
    private String word;

    @Pattern(regexp = "POLITICAL|VIOLENT|VULGAR|AD|OTHER", message = "分类不正确")
    private String category;

    // 0-停用 1-启用
    private Integer status;
}
