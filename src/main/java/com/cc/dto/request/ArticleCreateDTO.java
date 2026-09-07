package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/更新知识文章请求 DTO，字段与前端 ArticleDialog.vue 表单一一对应
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class ArticleCreateDTO {

    // 文章ID（创建时由前端生成的 UUID）
    @Size(max = 36, message = "文章ID长度不能超过36个字符")
    private String id;

    // 文章标题
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200个字符")
    private String title;

    // 文章内容（HTML）
    @NotBlank(message = "文章内容不能为空")
    private String content;

    // 封面图片
    @Size(max = 500, message = "封面图片路径长度不能超过500个字符")
    private String coverImage;

    // 分类ID
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    // 文章摘要
    @Size(max = 1000, message = "文章摘要长度不能超过1000个字符")
    private String summary;

    // 标签（逗号分隔，如 "焦虑,冥想"）
    @Size(max = 500, message = "标签长度不能超过500个字符")
    private String tags;
}
