package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户文章浏览历史实体类，对应数据库 user_browse_history 表
 *
 * <p>同一用户重复浏览同一文章时通过唯一索引兜底，仅更新 created_at 时间，
 * 不新增重复记录。</p>
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
@TableName("user_browse_history")
public class UserBrowseHistory {

    // 历史记录ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long userId;

    // 文章ID（UUID）
    @TableField("article_id")
    private String articleId;

    // 最近浏览时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}
