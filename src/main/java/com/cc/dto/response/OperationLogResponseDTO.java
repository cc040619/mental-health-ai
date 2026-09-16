package com.cc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员操作日志响应 DTO（内容已脱敏，直接展示）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class OperationLogResponseDTO {

    private Long id;

    private String operatorName;

    private String actionType;

    private String actionContent;

    private String ip;

    private LocalDateTime createdAt;
}
