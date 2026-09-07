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
 * 知识库文章实体类，对应数据库 knowledge_article 表
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@TableName("knowledge_article")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle {
    // 文章ID(UUID，前端创建时生成)
    @TableId(type = IdType.INPUT)
    private String id;

    // 分类ID
    @TableField("category_id")
    private Long categoryId;

    // 文章标题
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200个字符")
    private String title;

    // 文章摘要
    @TableField("summary")
    private String summary;

    // 文章内容(HTML)
    @NotBlank(message = "文章内容不能为空")
    private String content;

    // 封面图片
    @Size(max = 500, message = "封面图片路径长度不能超过500个字符")
    @TableField("cover_image")
    private String coverImage;

    // 标签(逗号分隔)
    @Size(max = 500, message = "标签长度不能超过500个字符")
    private String tags;

    // 作者ID
    @TableField("author_id")
    private Long authorId;

    // 阅读次数
    @TableField("read_count")
    private Integer readCount;

    // 状态 0:草稿 1:已发布 2:已下线
    private Integer status;

    // 发布时间
    @TableField("published_at")
    private LocalDateTime publishedAt;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
