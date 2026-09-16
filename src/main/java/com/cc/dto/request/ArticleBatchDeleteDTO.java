package com.cc.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章批量删除 DTO
 *
 * <p>物理删除，外键 CASCADE 自动清理收藏/浏览历史/点赞/评论。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ArticleBatchDeleteDTO {

    // 文章ID列表（UUID）
    @NotEmpty(message = "文章ID列表不能为空")
    @Size(max = 100, message = "单次批量操作不能超过100条")
    private List<String> ids;
}
