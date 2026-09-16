package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 咨询师收入统计 DTO（付费预留）。
 * 收入口径：已完成预约数 × price；price 未设置时 totalIncome 返回 null，前端展示「未设置」。
 *
 * <p>ponytail: 接入真实支付后，将统计口径替换为支付流水实付金额汇总，本 DTO 结构可保留。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ConsultantIncomeResponseDTO {

    /** 累计已完成预约数 */
    private Long totalCompleted;

    /** 近 30 天已完成预约数（按预约日期） */
    private Long monthCompleted;

    /** 估算总收入 = totalCompleted × price；price 未设置为 null */
    private BigDecimal totalIncome;

    /** 平均评分（取冗余 rating） */
    private BigDecimal avgRating;
}
