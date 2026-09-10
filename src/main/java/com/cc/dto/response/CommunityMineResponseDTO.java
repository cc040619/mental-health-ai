package com.cc.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 我的社区响应 DTO：我的帖子 / 我的评论 / 我的收藏 三组
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class CommunityMineResponseDTO {

    // 我发布的帖子（含举报待审/已下架状态，本人可见）
    private List<PostResponseDTO> posts;

    // 我发表的评论（携带所属帖子标题）
    private List<CommentResponseDTO> comments;

    // 我收藏的帖子
    private List<PostResponseDTO> favorites;
}
