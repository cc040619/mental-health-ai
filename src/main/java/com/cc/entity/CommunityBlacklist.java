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
 * 社区黑名单实体，对应 community_blacklist 表。
 * <p>屏蔽后仅禁止新发帖/评论（BusinessException 拒绝），不影响登录、不删历史内容。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("community_blacklist")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String reason;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
