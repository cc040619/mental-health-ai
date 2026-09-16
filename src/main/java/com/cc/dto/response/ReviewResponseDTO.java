package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 咨询师评价列表 DTO（匿名展示，reviewerName 固定为「来访者」）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class ReviewResponseDTO {

    private Long id;

    private Integer rating;

    private String content;

    /** 匿名展示名 */
    private String reviewerName;

    /** 管理员/咨询师回复 */
    private String replyContent;

    private LocalDateTime replyAt;

    private LocalDateTime createdAt;
}
