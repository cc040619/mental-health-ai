package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 敏感词批量导入请求（≤1000/批，跳过重复，事务批量插入）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class ImportWordsDTO {

    @NotEmpty(message = "导入词列表不能为空")
    @Size(max = 1000, message = "单次导入不能超过1000个词")
    private List<String> words;

    @Pattern(regexp = "POLITICAL|VIOLENT|VULGAR|AD|OTHER", message = "分类不正确")
    private String category;
}
