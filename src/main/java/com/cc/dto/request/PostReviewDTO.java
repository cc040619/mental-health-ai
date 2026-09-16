package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 帖子审核请求：仅待审核（REVIEW_PENDING）可审；驳回时原因必填
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class PostReviewDTO {

    // true-通过（→正常） false-驳回（→已驳回 + rejectReason）
    @NotNull(message = "审核结论不能为空")
    private Boolean approve;

    @Size(max = 500, message = "驳回原因不能超过500字")
    private String rejectReason;
}
