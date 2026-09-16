package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库分类响应 DTO，前端将列表映射为 {label: categoryName, value: id}
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDTO {

    // 分类ID
    private Long id;

    // 父分类ID
    private Long parentId;

    // 分类名称
    private String categoryName;

    // 分类描述
    private String description;

    // 分类图标
    private String icon;

    // 排序
    private Integer sortOrder;
}
