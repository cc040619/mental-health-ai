package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端板块列表项 DTO（含帖子数与版主昵称）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminBoardResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String icon;
    private String description;
    private Integer sortOrder;
    private Long moderatorId;
    private String moderatorName;

    // 板块帖子数（不含已删除）
    private Long postCount;

    // 0-停用 1-启用
    private Integer status;

    private LocalDateTime createdAt;
}
