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
 * 咨询师评价实体，对应 consultant_review 表。
 * uk_appointment 唯一键保证一单一评。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName("consultant_review")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("consultant_id")
    private Long consultantId;

    @TableField("user_id")
    private Long userId;

    /** 评分 1-5 */
    private Integer rating;

    private String content;

    /** 管理员/咨询师回复（单级） */
    @TableField("reply_content")
    private String replyContent;

    @TableField("reply_at")
    private LocalDateTime replyAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
