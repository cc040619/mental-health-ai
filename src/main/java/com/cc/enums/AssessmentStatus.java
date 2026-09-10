package com.cc.enums;

import lombok.Getter;

/**
 * 测评记录状态枚举
 *
 * @author cc
 * @date 2026-09-08
 */
@Getter
public enum AssessmentStatus {

    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String description;

    AssessmentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
