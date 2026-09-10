package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 社区举报请求 DTO
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class ReportCreateDTO {

    // 举报对象类型 1:帖子 2:评论
    @NotNull(message = "举报对象类型不能为空")
    private Integer targetType;

    // 举报对象ID
    @NotNull(message = "举报对象不能为空")
    private Long targetId;

    // 举报原因（≤300字）
    @NotBlank(message = "请填写举报原因")
    @Size(max = 300, message = "举报原因不能超过300个字符")
    private String reason;
}
