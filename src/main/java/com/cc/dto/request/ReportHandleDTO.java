package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员处理举报请求 DTO
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class ReportHandleDTO {

    // 处理动作：DOWN-下架内容  IGNORE-忽略举报
    @NotBlank(message = "处理动作不能为空")
    @Pattern(regexp = "DOWN|IGNORE", message = "处理动作仅支持 DOWN 或 IGNORE")
    private String action;
}
