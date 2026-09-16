package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程评论回复 DTO（管理员单级回复，重复回复覆盖并更新 replyAt）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CommentReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容不能超过500字")
    private String content;
}
