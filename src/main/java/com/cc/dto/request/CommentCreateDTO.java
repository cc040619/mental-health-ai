package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 社区评论 / 楼中楼回复请求 DTO
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class CommentCreateDTO {

    // 评论内容（≤1000字）
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;

    // 父评论ID（楼中楼回复时传入，限一层；顶层评论为 null）
    private Long parentId;

    // 是否匿名 0:否 1:是
    private Integer isAnonymous = 0;
}
