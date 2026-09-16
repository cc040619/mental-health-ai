package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库分类实体类，对应数据库 knowledge_category 表
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@TableName("knowledge_category")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCategory {
    // 分类ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 父分类ID
    @TableField("parent_id")
    private Long parentId;

    // 分类名称
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    @TableField("category_name")
    private String categoryName;

    // 分类代码
    @TableField("category_code")
    private String categoryCode;

    // 分类描述
    private String description;

    // 分类图标(emoji 或图片 URL)
    @Size(max = 200, message = "分类图标长度不能超过200个字符")
    private String icon;

    // 排序
    @TableField("sort_order")
    private Integer sortOrder;

    // 状态 0:禁用 1:启用
    private Integer status;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
