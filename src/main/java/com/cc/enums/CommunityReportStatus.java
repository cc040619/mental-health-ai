package com.cc.enums;

import lombok.Getter;

/**
 * 社区举报处理状态枚举
 *
 * @author cc
 * @date 2026-09-10
 */
@Getter
public enum CommunityReportStatus {

    PENDING(0, "待处理"),
    TAKEN_DOWN(1, "已下架"),
    IGNORED(2, "已忽略"),
    WARNED(3, "已警告");

    private final int code;
    private final String label;

    CommunityReportStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
