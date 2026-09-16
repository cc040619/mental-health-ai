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
 * 对话质量抽检实体
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_quality_sample")
public class AiQualitySample {

    /** 待标注 */
    public static final int STATUS_PENDING = 0;
    /** 好 */
    public static final int STATUS_GOOD = 1;
    /** 一般 */
    public static final int STATUS_NORMAL = 2;
    /** 坏 */
    public static final int STATUS_BAD = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Integer status;

    private Integer score;

    private String note;

    private Long annotatorId;

    private LocalDateTime annotatedAt;

    private LocalDateTime createdAt;
}
