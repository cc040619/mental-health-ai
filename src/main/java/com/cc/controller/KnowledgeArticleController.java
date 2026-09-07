package com.cc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.common.Result;
import com.cc.dto.request.ArticleCreateDTO;
import com.cc.dto.request.ArticlePageQueryDTO;
import com.cc.dto.request.ArticleStatusUpdateDTO;
import com.cc.dto.response.ArticleResponseDTO;
import com.cc.dto.response.ArticleSimpleResponseDTO;
import com.cc.service.KnowledgeArticleService;
import com.cc.util.CurrentUserUtils;
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

import java.util.Map;

/**
 * 知识文章控制器
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/knowledge/article")
public class KnowledgeArticleController {

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    // 分页查询文章列表（管理端全量筛选；前端列表带 sortField 时仅返回已发布文章）
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid ArticlePageQueryDTO queryDTO) {
        IPage<ArticleSimpleResponseDTO> page = knowledgeArticleService.page(queryDTO);
        return Result.ok(Map.of("records", page.getRecords(), "total", page.getTotal()));
    }

    // 查询文章详情（阅读次数自增）
    @GetMapping("/{id}")
    public Result<ArticleResponseDTO> detail(@PathVariable String id) {
        return Result.ok(knowledgeArticleService.detail(id));
    }

    // 创建文章（默认草稿，管理员）
    @PostMapping
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> create(@Valid @RequestBody ArticleCreateDTO createDTO) {
        Long authorId = CurrentUserUtils.getCurrentUserId();
        String id = knowledgeArticleService.create(authorId, createDTO);
        return Result.ok(Map.of("id", id));
    }

    // 更新文章（管理员）
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> update(@PathVariable String id, @Valid @RequestBody ArticleCreateDTO createDTO) {
        knowledgeArticleService.update(id, createDTO);
        return Result.ok();
    }

    // 删除文章（管理员）
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeArticleService.delete(id);
        return Result.ok();
    }

    // 更新文章状态（管理员）
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('2')")
    public Result<Void> updateStatus(@PathVariable String id, @Valid @RequestBody ArticleStatusUpdateDTO statusDTO) {
        knowledgeArticleService.updateStatus(id, statusDTO.getStatus());
        return Result.ok();
    }
}
