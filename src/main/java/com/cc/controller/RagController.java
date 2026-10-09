package com.cc.controller;

import com.cc.AIservice.rag.RagIndexService;
import com.cc.AIservice.rag.RagSearchService;
import com.cc.common.Result;
import com.cc.dto.response.RagSearchResultDTO;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 站内资料检索控制器（与 AI 对话工具、MCP 工具同一实现，便于自测与前端后续复用）
 *
 * <p>参数校验与可见性过滤统一在 {@link RagSearchService} 内完成（关键词必填且 ≤50 字、
 * 仅已发布文章与正常帖子、危机帖不返回）。</p>
 *
 * @author cc
 * @date 2026-09-27
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Resource
    private RagSearchService ragSearchService;

    @Resource
    private RagIndexService ragIndexService;

    /** 检索站内资料：type=article|post|all（默认 all），topK 1-5（默认 3） */
    @GetMapping("/search")
    public Result<List<RagSearchResultDTO>> search(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = RagSearchService.TYPE_ALL) String type,
            @RequestParam(required = false, defaultValue = "3") Integer topK) {
        return Result.ok(ragSearchService.search(keyword, type, topK));
    }

    /**
     * 全量重建向量索引并落盘（管理员）
     *
     * <p>重建会调用 embedding 接口产生费用，故不提供自动/定时触发，只由管理员显式发起。</p>
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> reindex() {
        return Result.ok(ragIndexService.rebuild());
    }

    /** 索引状态（管理员）：文档数/构建时间/文件是否存在，用于判断是否需要重建 */
    @GetMapping("/index/status")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> indexStatus() {
        return Result.ok(ragIndexService.status());
    }
}