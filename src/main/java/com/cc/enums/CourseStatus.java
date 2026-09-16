package com.cc.enums;

import lombok.Getter;

/**
 * 心理课程审核流状态枚举（五态）
 *
 * <p>流转规则：</p>
 * <ul>
 *   <li>DRAFT(0 草稿) / REJECTED(3 已驳回) → 提交审核 → PENDING(1 待审核)</li>
 *   <li>PENDING(1 待审核) → 审核通过 → PUBLISHED(2 已发布)；驳回 → REJECTED(3)</li>
 *   <li>PUBLISHED(2 已发布) ↔ OFFLINE(4 已下架) 互相切换（上下架）</li>
 * </ul>
 *
 * <p>咨询师发布课程为后续扩展：本模块由管理员统一维护，
 * 状态机设计已兼容「讲师提交 → 管理员审核」场景。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Getter
public enum CourseStatus {

    DRAFT(0, "草稿"),
    PENDING(1, "待审核"),
    PUBLISHED(2, "已发布"),
    REJECTED(3, "已驳回"),
    OFFLINE(4, "已下架");

    private final Integer code;
    private final String label;

    CourseStatus(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据代码获取枚举，未匹配返回 null（不抛异常，便于外部入参校验）
     */
    public static CourseStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CourseStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
