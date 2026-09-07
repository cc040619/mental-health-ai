package com.cc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新文章状态请求 DTO，对应前端 body { status: number }
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class ArticleStatusUpdateDTO {

    // 状态 0:草稿 1:发布 2:下线
    @NotNull(message = "状态不能为空")
    private Integer status;
}
