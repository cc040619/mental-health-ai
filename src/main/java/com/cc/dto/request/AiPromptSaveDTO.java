package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 提示词保存新版本 DTO（一律存草稿，激活才生效——两步防误操作）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AiPromptSaveDTO {

    @NotBlank(message = "提示词 key 不能为空")
    @Size(max = 50, message = "提示词 key 长度不能超过50")
    private String promptKey;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String name;

    @NotBlank(message = "提示词内容不能为空")
    @Size(max = 8000, message = "提示词内容长度不能超过8000")
    private String content;
}
