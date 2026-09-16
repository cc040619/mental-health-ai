package com.cc.dto.response;

import lombok.Data;

/**
 * 分类列表项（含文章数量统计），管理端分类管理使用
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CategoryWithCountResponseDTO {

    // 分类ID
    private Long id;

    // 父分类ID
    private Long parentId;

    // 分类名称
    private String categoryName;

    // 分类代码
    private String categoryCode;

    // 分类描述
    private String description;

    // 分类图标
    private String icon;

    // 排序
    private Integer sortOrder;

    // 状态 0:禁用 1:启用
    private Integer status;

    // 文章数量
    private Long articleCount;
}
