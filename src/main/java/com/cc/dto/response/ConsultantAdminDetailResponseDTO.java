package com.cc.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端咨询师详情 DTO：含资质图片、审核信息、排序权重、价格，仅管理员可见。
 *
 * @author cc
 * @date 2026-09-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConsultantAdminDetailResponseDTO extends ConsultantDetailResponseDTO {

    /** 0-待审核 1-正常 2-禁用 */
    private Integer status;

    /** 资质证明图片URL数组 */
    private List<String> qualificationImages;

    /** 驳回原因（非空=已驳回待修改） */
    private String rejectReason;

    /** 最近审核时间 */
    private LocalDateTime reviewedAt;

    /** 入驻时间（审核通过时间） */
    private LocalDateTime joinedAt;

    /** 排序权重 */
    private Integer sortWeight;

    /** 咨询价格(元/小时，付费预留)，null=未设置 */
    private BigDecimal price;
}
