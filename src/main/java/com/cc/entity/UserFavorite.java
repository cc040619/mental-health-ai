package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏实体类，对应数据库 user_favorite 表
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
@TableName("user_favorite")
public class UserFavorite {

    // 收藏ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 文章ID（UUID）
    @TableField("article_id")
    private String articleId;

    // 收藏时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
