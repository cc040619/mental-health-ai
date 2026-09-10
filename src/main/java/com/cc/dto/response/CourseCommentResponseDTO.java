package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程评论响应 DTO（分页列表项）
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CourseCommentResponseDTO {

    // 评论ID
    private Long id;

    // 课程ID
    private Long courseId;

    // 评论人ID
    private Long userId;

    // 评论内容
    private String content;

    // 评论时间
    private LocalDateTime createdAt;

    // 评论人昵称
    private String nickname;

    // 评论人头像
    private String avatar;

    // 是否本人（本人可删）
    private Boolean isMine;
}
