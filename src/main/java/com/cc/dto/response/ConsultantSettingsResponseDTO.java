package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 咨询师「我的预约设置」响应 DTO。
 *
 * @author cc
 * @date 2026-09-16
 */
@Data
public class ConsultantSettingsResponseDTO {

    /** 咨询价格（元/次）；null = 未设置（不收费） */
    private BigDecimal price;

    /** 可预约天数：用户端可约今天起未来 N 天（含今天） */
    private Integer bookingWindowDays;

    /** 用户取消时限：开始前 N 小时内限制用户取消，0 = 不限 */
    private Integer cancelHoursBefore;
}
