package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 咨询师工作台 - 待办提醒（三组各取前 5）。
 * upcoming=今天已确认且未开始；unrepliedReviews=近 30 天未回复评价；pendingAppointments=待确认申请。
 * 用户信息只含昵称（隐私边界：不向咨询师暴露手机号/邮箱）。
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class WorkbenchTodosResponseDTO {

    /** 即将开始的咨询（今天 CONFIRMED 且 start_time ≥ now，按时间升序，≤5） */
    private List<UpcomingItem> upcoming;

    /** 未回复评价（近 30 天，reply_content IS NULL，按时间倒序，≤5） */
    private List<UnrepliedReviewItem> unrepliedReviews;

    /** 待处理预约申请（PENDING，按创建时间升序，≤5） */
    private List<PendingAppointmentItem> pendingAppointments;

    @Data
    public static class UpcomingItem {
        private Long appointmentId;
        /** HH:mm */
        private String startTime;
        /** HH:mm */
        private String endTime;
        private String userNickname;
    }

    @Data
    public static class UnrepliedReviewItem {
        private Long reviewId;
        private Long appointmentId;
        private String userNickname;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    public static class PendingAppointmentItem {
        private Long appointmentId;
        private String userNickname;
        private LocalDate appointmentDate;
        /** HH:mm */
        private String startTime;
        private LocalDateTime createdAt;
    }
}
