package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端咨询评价列表项 DTO（后台可见真实用户昵称，前台评价列表匿名）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminReviewResponseDTO {

    private Long id;
    private Long appointmentId;
    private Long consultantId;
    private String consultantName;
    private Long userId;
    private String userNickname;
    private Integer rating;
    private String content;
    private String replyContent;
    private LocalDateTime replyAt;
    private LocalDateTime createdAt;
    private LocalDate appointmentDate;
}
