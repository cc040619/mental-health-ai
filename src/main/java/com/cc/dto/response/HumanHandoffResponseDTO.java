package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 转人工引导响应 DTO：咨询师预约模块上线后 handoffAvailable=true，
 * 前端据 redirectPath 跳转预约咨询师列表。
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@Builder
public class HumanHandoffResponseDTO {

    // 人工咨询是否可用（预约咨询模块已上线）
    private Boolean handoffAvailable;

    // 共情引导文案
    private String message;

    // 心理援助热线
    private String hotline;

    // 专业求助建议
    private String suggestion;

    // 预约咨询师页面路径（handoffAvailable=true 时前端跳转）
    private String redirectPath;
}
