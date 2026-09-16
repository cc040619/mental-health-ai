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
 * AI 提示词模板实体（key 维度多版本，同 key 仅一行 status=1 生效）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_prompt_template")
public class AiPromptTemplate {

    /** 生效 */
    public static final int STATUS_ACTIVE = 1;

    /** 历史/草稿 */
    public static final int STATUS_INACTIVE = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String promptKey;

    private String name;

    private String content;

    private Integer version;

    private Integer status;

    private LocalDateTime createdAt;
}
