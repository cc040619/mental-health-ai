package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章点赞实体类，对应数据库 article_like 表
 *
 * <p>唯一索引 (user_id, article_id) 兜底防重复点赞。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("article_like")
public class ArticleLike {

    // 点赞ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 文章ID(UUID)
    @TableField("article_id")
    private String articleId;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 点赞时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
