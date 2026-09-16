package com.cc.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 轮播图新增 / 编辑请求 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class BannerSaveDTO {

    @Size(max = 100, message = "标题不能超过100个字符")
    private String title;

    // 图片地址（OSS 上传产物 URL）
    @NotBlank(message = "图片不能为空")
    @Size(max = 500, message = "图片地址长度异常")
    private String imageUrl;

    // 跳转链接（可空；仅 http/https，Service 层白名单校验防伪协议）
    @Size(max = 500, message = "链接长度异常")
    private String linkUrl;

    // 排序（小在前）
    @Min(value = 0, message = "排序不能为负数")
    private Integer sortOrder;
}
