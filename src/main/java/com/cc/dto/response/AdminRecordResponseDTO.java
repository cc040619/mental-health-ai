package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端测评记录列表项响应 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminRecordResponseDTO {

    private Long id;
    private Long userId;
    private String userNickname;
    private Long scaleId;
    private String scaleName;
    private BigDecimal totalScore;
    private String level;
    private String levelLabel;
    private Integer riskFlag;
    private LocalDateTime completedAt;
}
