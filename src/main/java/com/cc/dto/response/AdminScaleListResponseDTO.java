package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端量表列表项响应 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminScaleListResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String category;
    private String categoryLabel;
    private Integer questionCount;
    private Integer estimatedMinutes;
    private Integer assessmentCount; // 测评次数（LEFT JOIN 聚合）
    private Integer enabled; // 1启用 0停用
    private String enabledLabel;
    private LocalDateTime createdAt;
}
