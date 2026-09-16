package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 章节批量重排 DTO（单事务一次性更新整课排序）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ChapterSortDTO {

    @NotEmpty(message = "排序列表不能为空")
    @Size(max = 200, message = "单次排序章节数不能超过200")
    @Valid
    private List<SortItem> items;

    @Data
    public static class SortItem {
        private Long id;
        private Integer sortOrder;
    }
}
