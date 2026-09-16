package com.cc.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章批量移动分类 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ArticleBatchCategoryDTO {

    // 文章ID列表（UUID）
    @NotEmpty(message = "文章ID列表不能为空")
    @Size(max = 100, message = "单次批量操作不能超过100条")
    private List<String> ids;

    // 目标分类ID
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}
