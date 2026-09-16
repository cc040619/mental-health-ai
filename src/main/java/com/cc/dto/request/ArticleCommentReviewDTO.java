package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文章评论审核 DTO
 *
 * <p>approve=true → status=1(已通过) + comment_count+1；
 * approve=false → status=2(已拒绝)。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ArticleCommentReviewDTO {

    // 是否审核通过
    @NotNull(message = "审核结果不能为空")
    private Boolean approve;
}
