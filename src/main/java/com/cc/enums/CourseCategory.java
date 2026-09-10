package com.cc.enums;

import lombok.Getter;

/**
 * 心理课程类别枚举
 *
 * @author cc
 * @date 2026-09-09
 */
@Getter
public enum CourseCategory {

    MEDITATION("MEDITATION", "正念冥想"),
    SLEEP("SLEEP", "睡眠助眠"),
    EMOTION("EMOTION", "情绪管理"),
    CBT("CBT", "认知行为"),
    INTERPERSONAL("INTERPERSONAL", "人际关系");

    private final String code;
    private final String label;

    CourseCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static CourseCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CourseCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
