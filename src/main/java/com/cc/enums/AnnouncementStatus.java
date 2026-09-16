package com.cc.enums;

import lombok.Getter;

/**
 * 公告状态枚举（三态，落库 announcement.status）
 *
 * <p>状态机：DRAFT → PUBLISHED → WITHDRAWN（撤回后可重新发布或编辑/删除）；
 * 已发布公告必须先撤回才能编辑/删除（内容安全：线上公告不允许静默篡改）。
 * 存量数据迁移：旧 0(已下架) → 2(已撤回)。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Getter
public enum AnnouncementStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    WITHDRAWN(2, "已撤回");

    private final int code;
    private final String label;

    AnnouncementStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AnnouncementStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AnnouncementStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
