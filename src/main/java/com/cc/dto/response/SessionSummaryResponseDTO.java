package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话总结响应 DTO
 *
 * <p>同时作为 consultation_session.summary 列的 JSON 存储结构（status 字段不入库，
 * 由读取方按是否存在总结推导：READY-已生成 / GENERATING-生成中）。</p>
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryResponseDTO {

    // 状态：READY-已生成 / GENERATING-生成中
    private String status;

    // 会话摘要（150字内）
    private String summary;

    // 核心问题列表（2-4条）
    private List<String> coreIssues;

    // 建议清单（3-5条）
    private List<String> suggestions;

    // 生成时间戳（毫秒）
    private Long generatedAt;
}
