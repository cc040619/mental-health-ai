package com.cc.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 咨询师「我的预约设置」请求 DTO。
 * price 为空 = 不收费（咨询价格展示按未设置处理）。
 *
 * @author cc
 * @date 2026-09-16
 */
@Data
public class ConsultantSettingsDTO {

    /** 咨询价格（元/次），空 = 不收费 */
    @DecimalMin(value = "0", message = "咨询价格不能为负数")
    private BigDecimal price;

    /** 可预约天数：用户端可约今天起未来 N 天（含今天） */
    @NotNull(message = "可预约天数不能为空")
    @Min(value = 1, message = "可预约天数至少为1天")
    @Max(value = 90, message = "可预约天数不能超过90天")
    private Integer bookingWindowDays;

    /** 用户取消时限：开始前 N 小时内限制用户取消，0 = 不限；咨询师/管理员取消不受此限 */
    @NotNull(message = "取消时限不能为空")
    @Min(value = 0, message = "取消时限不能为负数")
    @Max(value = 48, message = "取消时限不能超过48小时")
    private Integer cancelHoursBefore;
}
