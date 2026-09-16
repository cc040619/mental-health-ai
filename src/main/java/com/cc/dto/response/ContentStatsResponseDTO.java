package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内容数据统计响应 DTO：文章浏览 / 课程学习 / 热门帖子 各 TOP10（与时间范围无关）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentStatsResponseDTO {

    // 文章浏览量 TOP10
    private List<NameCountDTO> articles;

    // 课程学习量 TOP10
    private List<NameCountDTO> courses;

    // 热门帖子 TOP10（权重 = 点赞数 + 评论数）
    private List<NameCountDTO> posts;
}
