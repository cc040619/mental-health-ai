package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端课程评论列表项响应 DTO
 *
 * <p>前台展示回复时复用此 DTO 的 replyContent/replyAt 字段，无需额外接口。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminCourseCommentResponseDTO {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private String replyContent;
    private LocalDateTime replyAt;
    private LocalDateTime createdAt;
    // 是否已回复（前端展示 tag）
    private Boolean hasReply;
}
