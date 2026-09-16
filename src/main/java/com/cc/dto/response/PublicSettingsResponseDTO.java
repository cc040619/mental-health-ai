package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 公开系统设置响应 DTO（白名单 key，不含任何开关配置）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class PublicSettingsResponseDTO {

    private String systemName;

    private String systemLogo;

    private String icpNumber;

    private String customerService;

    private String systemIntro;
}
