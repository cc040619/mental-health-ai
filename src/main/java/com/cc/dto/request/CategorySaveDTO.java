package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增/编辑请求 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CategorySaveDTO {

    // 父分类ID（0=顶级）
    private Long parentId;

    // 分类名称
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    private String categoryName;

    // 分类代码
    @Size(max = 50, message = "分类代码长度不能超过50个字符")
    private String categoryCode;

    // 分类描述
    private String description;

    // 分类图标(emoji 或图片 URL)
    @Size(max = 200, message = "分类图标长度不能超过200个字符")
    private String icon;

    // 排序
    private Integer sortOrder;

    // 状态 0:禁用 1:启用
    private Integer status;
}
