package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约 DTO：用户端/后台列表与详情共用。
 * 时间字段输出规范化字符串（HH:mm），用户信息仅后台分页 SQL 填充。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class AppointmentResponseDTO {

    private Long id;

    private Long userId;

    private Long consultantId;

    private Long slotId;

    private LocalDate appointmentDate;

    /** HH:mm */
    private String startTime;

    /** HH:mm */
    private String endTime;

    private String problemDesc;

    /** 取消原因 */
    private String cancelReason;

    /** PENDING/CONFIRMED/COMPLETED/CANCELED */
    private String status;

    /** 状态中文标签 */
    private String statusLabel;

    private Boolean evaluated;

    private LocalDateTime createdAt;

    // ===== 咨询师信息（关联查询填充） =====

    private String consultantName;

    private String consultantAvatar;

    private String consultantTitle;

    // ===== 预约用户信息（仅后台列表填充） =====

    private String userNickname;

    private String userPhone;

    // ===== 操作权限标记（service 按当前用户+状态+时间计算） =====

    /** 是否可取消：PENDING 可取消；CONFIRMED 且未到开始时间可取消 */
    private Boolean canCancel;

    /** 是否可评价：COMPLETED 且未评价且本人 */
    private Boolean canReview;

    // ===== 评价信息（仅后台详情填充） =====

    private Integer reviewRating;

    private String reviewContent;

    private String reviewReply;
}
