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
 * 社区举报实体，对应 community_report 表
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
@TableName("community_report")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 1-帖子 2-评论
    @TableField("target_type")
    private Integer targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("reporter_id")
    private Long reporterId;

    private String reason;

    // 0-待处理 1-已下架 2-已忽略 3-已警告
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("handled_at")
    private LocalDateTime handledAt;
}
