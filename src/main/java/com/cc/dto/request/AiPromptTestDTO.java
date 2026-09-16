package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提示词试跑 DTO（用指定提示词内容无记忆单轮调用，供编辑预览）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AiPromptTestDTO {

    @NotBlank(message = "用户消息不能为空")
    @Size(max = 1000, message = "用户消息长度不能超过1000")
    private String message;

    @NotBlank(message = "提示词内容不能为空")
    @Size(max = 8000, message = "提示词内容长度不能超过8000")
    private String content;
}
