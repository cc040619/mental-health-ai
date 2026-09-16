package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 回复拦截记录响应 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AiBlockRecordResponseDTO {

    private Long id;

    private Long sessionId;

    private String triggerWord;

    private String contentSummary;

    private String action;

    private LocalDateTime createdAt;
}
