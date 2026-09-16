package com.cc.enums;

import com.cc.exception.BusinessException;

/**
 * 预约状态机：PENDING(待确认) → CONFIRMED(已确认) → COMPLETED(已完成)；
 * PENDING/CONFIRMED 可 → CANCELED(已取消)；COMPLETED/CANCELED 为终态。
 *
 * <p>所有状态变更必须经 {@link #canTransitTo(AppointmentStatus)} 硬校验，
 * 非法流转抛 {@link BusinessException}，禁止静默改状态。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
public enum AppointmentStatus {

    PENDING("待确认"),
    CONFIRMED("已确认"),
    COMPLETED("已完成"),
    CANCELED("已取消");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 状态机合法流转表
     *
     * @param target 目标状态
     * @return true=允许流转
     */
    public boolean canTransitTo(AppointmentStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELED;
            case CONFIRMED -> target == COMPLETED || target == CANCELED;
            // 终态不可再流转
            case COMPLETED, CANCELED -> false;
        };
    }

    /**
     * 解析状态码，非法值抛业务异常
     */
    public static AppointmentStatus fromCode(String code) {
        try {
            return AppointmentStatus.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("预约状态不正确");
        }
    }
}
