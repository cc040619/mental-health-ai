package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 咨询师端「会话列表」项 DTO。
 *
 * <p>会话=预约：本 DTO 即当前咨询师名下预约的卡片视图，附加最后一条留言摘要/留言数。
 * 隐私边界：仅含用户昵称，禁止出现手机号/邮箱。最后留言内容仅取摘要（≤50 字）。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class ConsultantSessionResponseDTO {

    /** 预约ID（会话 id） */
    private Long appointmentId;

    private Long userId;

    /** 来访者昵称 */
    private String userNickname;

    private LocalDate appointmentDate;

    /** HH:mm */
    private String startTime;

    /** HH:mm */
    private String endTime;

    /** PENDING/CONFIRMED/COMPLETED/CANCELED */
    private String status;

    /** 状态中文标签 */
    private String statusLabel;

    private String problemDesc;

    /** 最后一条留言内容摘要（≤50 字，避免泄露过多对话细节） */
    private String lastMessageContent;

    private LocalDateTime lastMessageTime;

    /** 留言总数 */
    private Long messageCount;

    private LocalDateTime createdAt;
}
