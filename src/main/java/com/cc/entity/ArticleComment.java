package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章评论实体类，对应数据库 article_comment 表
 *
 * <p>评论发表后默认 status=0 待审核，管理员审核通过后 status=1 才在前台展示；
 * status=2 为已拒绝。knowledge_article.comment_count 仅统计 status=1 的评论。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("article_comment")
public class ArticleComment {

    // 评论ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 文章ID(UUID)
    @TableField("article_id")
    private String articleId;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 评论内容
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500个字符")
    private String content;

    // 状态 0:待审核 1:已通过 2:已拒绝
    private Integer status;

    // 评论时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
