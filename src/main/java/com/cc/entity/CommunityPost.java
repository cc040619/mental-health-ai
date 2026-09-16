package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子实体，对应 community_post 表
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
@TableName(value = "community_post", autoResultMap = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 板块：TREES/ACADEMIC/RELATIONSHIP/SLEEP/RECOVERY/HELP
    private String board;

    private String title;

    // 正文（富文本 HTML，存库前经 HtmlSanitizer 白名单清洗）
    private String content;

    // 图片URL数组（JSON 列，JacksonTypeHandler 自动序列化）
    @TableField(value = "images", typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    @TableField("author_id")
    private Long authorId;

    @TableField("is_anonymous")
    private Integer isAnonymous;

    // 0-否 1-置顶（置顶不设时长，扩展路径：加 top_expire_at + 定时任务）
    @TableField("is_top")
    private Integer isTop;

    // 0-否 1-加精
    @TableField("is_featured")
    private Integer isFeatured;

    // 审核驳回原因（仅 REJECTED 状态有值）
    @TableField("reject_reason")
    private String rejectReason;

    // 0-正常 1-已删除 2-举报待审 3-已下架 4-待审核 5-已驳回
    private Integer status;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("favorite_count")
    private Integer favoriteCount;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("is_crisis")
    private Integer isCrisis;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
