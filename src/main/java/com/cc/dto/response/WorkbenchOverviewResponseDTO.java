package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 咨询师工作台 - 数据概览。
 * 口径：今日预约=今日 PENDING+CONFIRMED；待处理=PENDING；本月咨询=本月 COMPLETED；
 * 总收入=累计 COMPLETED 数 × 单价（price 未设置时 totalIncome=null 且 priceUnset=true）。
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class WorkbenchOverviewResponseDTO {

    /** 咨询师姓名（欢迎条展示） */
    private String consultantName;

    /** 今日预约数（待确认+已确认） */
    private Long todayAppointments;

    /** 待处理预约数（PENDING） */
    private Long pendingAppointments;

    /** 本月咨询次数（本月 COMPLETED） */
    private Long monthConsultCount;

    /** 估算总收入（完成数 × 单价）；未设置价格为 null */
    private BigDecimal totalIncome;

    /** true=未设置咨询价格（前端展示「未设置」） */
    private Boolean priceUnset;

    /** 平均评分（consultant.rating 冗余值） */
    private BigDecimal avgRating;

    /** 评价数（consultant.review_count 冗余值） */
    private Integer reviewCount;
}
