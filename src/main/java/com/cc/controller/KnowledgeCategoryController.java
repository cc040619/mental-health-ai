package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.response.CategoryResponseDTO;
import com.cc.service.KnowledgeCategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识分类控制器
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/knowledge/category")
public class KnowledgeCategoryController {

    @Resource
    private KnowledgeCategoryService knowledgeCategoryService;

    // 获取分类列表（前端映射为下拉选项 label/value）
    @GetMapping("/tree")
    public Result<List<CategoryResponseDTO>> tree() {
        return Result.ok(knowledgeCategoryService.listEnabled());
    }
}
