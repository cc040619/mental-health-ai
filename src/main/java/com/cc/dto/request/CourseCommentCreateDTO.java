package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程评论发表请求 DTO
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CourseCommentCreateDTO {

    // 评论内容（≤300字）
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 300, message = "评论内容不能超过300个字符")
    private String content;
}
