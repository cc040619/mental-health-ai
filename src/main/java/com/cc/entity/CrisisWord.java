package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 危机词库实体（表驱动，安全底线：启用词不足 5 个时删除/停用被拒绝）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("crisis_word")
public class CrisisWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    /** 0-停用 1-启用 */
    private Integer status;

    private LocalDateTime createdAt;
}
