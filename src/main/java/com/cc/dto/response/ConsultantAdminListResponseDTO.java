package com.cc.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理端咨询师列表行 DTO：在用户端卡片字段上扩展审核状态字段。
 * rejectReason 非空即「已驳回待修改」，前端用其做 tooltip 展示。
 *
 * @author cc
 * @date 2026-09-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConsultantAdminListResponseDTO extends ConsultantListResponseDTO {

    /** 0-待审核 1-正常 2-禁用 */
    private Integer status;

    /** 驳回原因（非空=已驳回待修改），仅管理端可见 */
    private String rejectReason;

    /** 最近审核时间 */
    private LocalDateTime reviewedAt;

    /** 入驻时间（审核通过时间） */
    private LocalDateTime joinedAt;
}
