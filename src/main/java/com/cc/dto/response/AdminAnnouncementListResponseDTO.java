package com.cc.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台公告列表响应 DTO（单条 SQL 聚合阅读人数，禁止 N+1）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AdminAnnouncementListResponseDTO {

    private Long id;

    private String title;

    // 编辑回显需要富文本内容（已清洗的 HTML）
    private String content;

    // SYSTEM/ACTIVITY/TIP
    private String type;

    // 0-草稿 1-已发布 2-已撤回
    private Integer status;

    // ALL-全体 USERS-指定用户
    private String targetType;

    // 指定用户 ID 列表（targetType=USERS 时回显）
    private List<Long> targetUserIds;

    // DB 原始 target_user_ids JSON（adminPage 解析用，不输出前端）
    @JsonIgnore
    private String targetUserIdsJson;

    // 指定用户昵称（逗号连接，列表展示「指定 N 人」的 tooltip）
    private String targetNames;

    // 定时发布时间
    private LocalDateTime publishAt;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    // 阅读人数（announcement_read 计数）
    private Long readCount;
}
