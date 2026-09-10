package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 社区评论实体，对应 community_comment 表（支持楼中楼，parent_id 限一层）
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
@TableName("community_comment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    // 父评论ID，顶层评论为 null
    @TableField("parent_id")
    private Long parentId;

    @TableField("author_id")
    private Long authorId;

    private String content;

    @TableField("is_anonymous")
    private Integer isAnonymous;

    // 0-正常 1-已删除
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
