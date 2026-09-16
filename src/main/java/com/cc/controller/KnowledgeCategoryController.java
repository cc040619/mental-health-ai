package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.CategorySaveDTO;
import com.cc.dto.response.CategoryResponseDTO;
import com.cc.dto.response.CategoryWithCountResponseDTO;
import com.cc.service.KnowledgeCategoryService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识分类控制器
 *
 * <p>前台 /tree 返回启用的分类树；后台 /list 返回全部分类含文章数量统计，
 * POST/PUT/DELETE 需管理员权限。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/knowledge/category")
public class KnowledgeCategoryController {

    @Resource
    private KnowledgeCategoryService knowledgeCategoryService;

    /**
     * 获取分类树（前台映射为下拉选项 label/value）
     */
    @GetMapping("/tree")
    public Result<List<CategoryResponseDTO>> tree() {
        return Result.ok(knowledgeCategoryService.listEnabled());
    }

    /**
     * 获取全部分类（含文章数量统计，后台分类管理使用）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('2')")
    public Result<List<CategoryWithCountResponseDTO>> list() {
        return Result.ok(knowledgeCategoryService.listAllWithCount());
    }

    /**
     * 新增分类（管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> create(@Valid @RequestBody CategorySaveDTO dto) {
        Long id = knowledgeCategoryService.create(dto);
        return Result.ok(Map.of("id", id));
    }

    /**
     * 更新分类（管理员，名称/描述/图标/排序/状态）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        knowledgeCategoryService.update(id, dto);
        return Result.ok();
    }

    /**
     * 删除分类（管理员，FK RESTRICT：有文章的分类不可删除）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeCategoryService.delete(id);
        return Result.ok();
    }
}
