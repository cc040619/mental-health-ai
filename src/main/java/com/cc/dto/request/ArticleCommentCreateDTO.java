package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章评论发表 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ArticleCommentCreateDTO {

    // 评论内容（发表前过敏感词过滤，命中即拒绝）
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500个字符")
    private String content;
}
