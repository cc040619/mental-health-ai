package com.cc.dto.request;

import lombok.Data;

/**
 * 文章分页查询 DTO
 *
 * <p>兼容管理端（currentPage/size/title/categoryId/status）与前端列表
 * （sortField/sortDirection/currentPage/size，带 sortField 时仅返回已发布文章）。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class ArticlePageQueryDTO {

    // 页码（管理端与前端列表统一使用 currentPage）
    private Long currentPage = 1L;

    // 每页条数
    private Long size = 10L;

    // 标题模糊搜索
    private String title;

    // 分类ID
    private Long categoryId;

    // 状态 0:草稿 1:已发布 2:已下线，空字符串表示全部
    private Integer status;

    // 排序字段 publishedAt|readCount（前端列表专用，出现时强制仅查询已发布文章）
    private String sortField;

    // 排序方向 asc|desc
    private String sortDirection;
}
