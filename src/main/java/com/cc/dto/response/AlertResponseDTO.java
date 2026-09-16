package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 高危预警列表项响应 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AlertResponseDTO {

    private Long id;
    private Long userId;
    private String userNickname;
    private Long scaleId;
    private String scaleName;
    private BigDecimal totalScore;
    private String level;
    private String levelLabel;
    private Integer riskFlag;
    private Integer interventionStatus;
    private String interventionStatusLabel;
    private String interventionNote;
    private LocalDateTime intervenedAt;
    private LocalDateTime completedAt;
}
