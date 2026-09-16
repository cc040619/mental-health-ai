package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 咨询师资质审核请求 DTO。
 * approve=true 审核通过；approve=false 驳回时 rejectReason 必填（Service 兜底校验）。
 * 注意：用户端评价提交已占用 {@link ConsultantReviewDTO} 类名，故审核 DTO 命名 Audit。
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ConsultantAuditDTO {

    /** true-通过 false-驳回 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approve;

    /** 驳回原因，approve=false 时必填，≤500 字 */
    @Size(max = 500, message = "驳回原因不能超过500字")
    private String rejectReason;
}
