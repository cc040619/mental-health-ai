package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDate;

/**
 * 咨询师端「会话详情」聚合 DTO：预约信息 + 来访者基本信息 + 咨询记录。
 *
 * <p>聚合一次返回，前端分区展示。隐私边界：仅含昵称/头像，不含手机号/邮箱。
 * 留言列表/测评/日记/历史记录走各自分页子接口，本 DTO 不内嵌。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class SessionDetailResponseDTO {

    private Long appointmentId;

    private Long userId;

    /** 来访者昵称 */
    private String userNickname;

    /** 来访者头像 */
    private String userAvatar;

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

    /** 咨询记录（无则 null，前端展示空模板） */
    private ConsultationRecordResponseDTO record;
}
