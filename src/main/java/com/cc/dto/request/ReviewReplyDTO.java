package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评价回复请求（管理员/咨询师单级回复）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ReviewReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容不能超过500字")
    private String content;
}
