package com.cc.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 文章数据统计响应 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ArticleStatsResponseDTO {

    // 近30天浏览趋势 [{name: 日期, value: 次数}]
    private List<NameCountDTO> dailyBrowses;

    // 平均阅读时长（秒）
    private Long avgDuration;

    // 累计阅读时长（秒）
    private Long totalDuration;

    // 总浏览人次（user_browse_history 记录数）
    private Long totalViews;

    // 阅读次数（文章冗余字段 read_count）
    private Integer readCount;

    // 点赞数
    private Integer likeCount;

    // 已通过评论数
    private Integer commentCount;

    // 收藏数
    private Integer favoriteCount;
}
