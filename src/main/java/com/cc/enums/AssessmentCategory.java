package com.cc.enums;

import lombok.Getter;

/**
 * 心理量表类别枚举
 *
 * @author cc
 * @date 2026-09-08
 */
@Getter
public enum AssessmentCategory {

    EMOTION("EMOTION", "情绪"),
    PERSONALITY("PERSONALITY", "人格"),
    STRESS("STRESS", "压力"),
    SLEEP("SLEEP", "睡眠"),
    INTERPERSONAL("INTERPERSONAL", "人际关系");

    private final String code;
    private final String description;

    AssessmentCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AssessmentCategory fromCode(String code) {
        for (AssessmentCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
