package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 社区发帖请求 DTO
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class PostCreateDTO {

    // 板块编码
    @NotBlank(message = "请选择板块")
    private String board;

    // 标题（≤80字）
    @NotBlank(message = "标题不能为空")
    @Size(max = 80, message = "标题不能超过80个字符")
    private String title;

    // 正文（富文本 HTML，后端白名单清洗）
    @NotBlank(message = "正文内容不能为空")
    private String content;

    // 图片URL数组（最多9张）
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> images;

    // 是否匿名发布 0:否 1:是
    private Integer isAnonymous = 0;
}
