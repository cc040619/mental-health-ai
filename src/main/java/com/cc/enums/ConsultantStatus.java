package com.cc.enums;

import com.cc.exception.BusinessException;

/**
 * 咨询师状态机：PENDING(0,待审核) / NORMAL(1,正常) / DISABLED(2,禁用)。
 *
 * <p>审核语义：驳回不新造状态——{@code PENDING + reject_reason 非空} 表示「已驳回待修改」，
 * 重新提交保存时清空驳回原因回到纯待审核。</p>
 * <ul>
 *   <li>PENDING → NORMAL（审核通过，填 reviewed_at/joined_at）</li>
 *   <li>PENDING → 驳回（状态仍为 PENDING，reject_reason 必填）</li>
 *   <li>NORMAL ↔ DISABLED（禁用/恢复）；禁用后用户端不可见、新预约不可选，已确认预约不受影响</li>
 * </ul>
 *
 * @author cc
 * @date 2026-09-15
 */
public enum ConsultantStatus {

    PENDING(0, "待审核"),
    NORMAL(1, "正常"),
    DISABLED(2, "禁用");

    private final int code;
    private final String label;

    ConsultantStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** 仅待审核记录可走审核接口（重复审核/已禁用记录拒绝） */
    public static boolean isReviewable(Integer code) {
        return code != null && code == PENDING.code;
    }

    /** 解析状态码，非法值抛业务异常 */
    public static ConsultantStatus fromCode(Integer code) {
        if (code != null) {
            for (ConsultantStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        throw new BusinessException("咨询师状态值不正确");
    }
}
