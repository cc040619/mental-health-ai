package com.cc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.common.Result;
import com.cc.dto.request.ArticleCommentReviewDTO;
import com.cc.dto.response.ArticleCommentResponseDTO;
import com.cc.service.ArticleCommentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 文章评论管理控制器（后台）
 *
 * <p>评论审核流：发表默认待审核(0) → 管理员审核通过(1)/拒绝(2)。
 * comment_count 仅在审核通过时 +1，删除已通过评论时 -1。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/knowledge/comment")
@PreAuthorize("hasRole('2')")
public class ArticleCommentController {

    @Resource
    private ArticleCommentService articleCommentService;

    /**
     * 后台分页查询评论（支持状态/文章筛选）
     *
     * @param status    评论状态 0/1/2，null=全部
     * @param articleId 文章ID，null=全部
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String articleId,
                                             @RequestParam(required = false) Long page,
                                             @RequestParam(required = false) Long size) {
        IPage<ArticleCommentResponseDTO> commentPage = articleCommentService.adminPage(status, articleId, page, size);
        return Result.ok(Map.of("records", commentPage.getRecords(), "total", commentPage.getTotal()));
    }

    /**
     * 审核评论（通过→status=1+comment_count+1，拒绝→status=2）
     */
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ArticleCommentReviewDTO dto) {
        articleCommentService.review(id, dto.getApprove());
        return Result.ok();
    }

    /**
     * 删除评论（物理删；若原 status=1 需 comment_count-1）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleCommentService.delete(id);
        return Result.ok();
    }
}
