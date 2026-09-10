package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 收藏操作请求 DTO
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
public class FavoriteOperateDTO {

    // 文章ID（UUID）
    @NotBlank(message = "文章ID不能为空")
    private String articleId;
}
