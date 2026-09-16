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
 * 社区板块实体，对应 community_board 表（板块由枚举转表驱动，存量帖子 board 字符串不变）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("community_board")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBoardEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 板块编码（对应 post.board）
    private String code;

    private String name;

    private String icon;

    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    // 版主用户ID（可选）
    @TableField("moderator_id")
    private Long moderatorId;

    // 0-停用 1-启用
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
