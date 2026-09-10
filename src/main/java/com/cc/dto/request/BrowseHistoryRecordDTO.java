package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 浏览历史记录请求 DTO（用户访问文章详情时上报）
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
public class BrowseHistoryRecordDTO {

    // 文章ID（UUID）
    @NotBlank(message = "文章ID不能为空")
    private String articleId;
}
