package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 转人工引导响应 DTO（模板实现：咨询师模块未上线，返回温和引导文案与求助资源）
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@Builder
public class HumanHandoffResponseDTO {

    // 人工咨询是否可用（咨询师模块未上线，固定 false）
    private Boolean handoffAvailable;

    // 共情引导文案
    private String message;

    // 心理援助热线
    private String hotline;

    // 专业求助建议
    private String suggestion;
}
