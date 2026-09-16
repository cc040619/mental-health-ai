package com.cc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.common.Result;
import com.cc.dto.request.ArticleBatchCategoryDTO;
import com.cc.dto.request.ArticleBatchDeleteDTO;
import com.cc.dto.request.ArticleBatchStatusDTO;
import com.cc.dto.request.ArticleCommentCreateDTO;
import com.cc.dto.request.ArticleCreateDTO;
import com.cc.dto.request.ArticlePageQueryDTO;
import com.cc.dto.request.ArticleStatusUpdateDTO;
import com.cc.dto.request.ReadDurationDTO;
import com.cc.dto.response.ArticleCommentResponseDTO;
import com.cc.dto.response.ArticleResponseDTO;
import com.cc.dto.response.ArticleSimpleResponseDTO;
import com.cc.dto.response.ArticleStatsResponseDTO;
import com.cc.service.ArticleCommentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识文章控制器
 *
 * <p>含管理端（分页/详情/增删改/批量操作/数据统计）与前台（点赞/评论/阅读时长上报）接口。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/knowledge/article")
public class KnowledgeArticleController {

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    @Resource
    private ArticleCommentService articleCommentService;

    // ==================== 管理端 ====================

    /**
     * 分页查询文章列表（管理端全量筛选；前端列表带 sortField 时仅返回已发布文章）
     * <p>管理端支持 sortBy（readCount/likeCount/commentCount/favoriteCount/createdAt）排序</p>
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid ArticlePageQueryDTO queryDTO) {
        IPage<ArticleSimpleResponseDTO> page = knowledgeArticleService.page(queryDTO);
        return Result.ok(Map.of("records", page.getRecords(), "total", page.getTotal()));
    }

    /**
     * 查询文章详情（阅读次数自增）
     */
    @GetMapping("/{id}")
    public Result<ArticleResponseDTO> detail(@PathVariable String id) {
        return Result.ok(knowledgeArticleService.detail(id));
    }

    /**
     * 创建文章（默认草稿，管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> create(@Valid @RequestBody ArticleCreateDTO createDTO) {
        Long authorId = CurrentUserUtils.getCurrentUserId();
        String id = knowledgeArticleService.create(authorId, createDTO);
        return Result.ok(Map.of("id", id));
    }

    /**
     * 更新文章（管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> update(@PathVariable String id, @Valid @RequestBody ArticleCreateDTO createDTO) {
        knowledgeArticleService.update(id, createDTO);
        return Result.ok();
    }

    /**
     * 删除文章（管理员，物理删除，外键 CASCADE 自动清理收藏/浏览/点赞/评论）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeArticleService.delete(id);
        return Result.ok();
    }

    /**
     * 更新文章状态（管理员，单条上架/下架）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('2')")
    public Result<Void> updateStatus(@PathVariable String id, @Valid @RequestBody ArticleStatusUpdateDTO statusDTO) {
        knowledgeArticleService.updateStatus(id, statusDTO.getStatus());
        return Result.ok();
    }

    /**
     * 批量上架/下架（管理员，ids≤100）
     */
    @PostMapping("/batch/status")
    @PreAuthorize("hasRole('2')")
    public Result<Void> batchUpdateStatus(@Valid @RequestBody ArticleBatchStatusDTO dto) {
        knowledgeArticleService.batchUpdateStatus(dto.getIds(), dto.getStatus());
        return Result.ok();
    }

    /**
     * 批量删除（管理员，物理删除，注明级联影响：收藏/浏览/点赞/评论一并删除）
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('2')")
    public Result<Void> batchDelete(@Valid @RequestBody ArticleBatchDeleteDTO dto) {
        knowledgeArticleService.batchDelete(dto.getIds());
        return Result.ok();
    }

    /**
     * 批量移动分类（管理员，校验目标分类存在）
     */
    @PutMapping("/batch/category")
    @PreAuthorize("hasRole('2')")
    public Result<Void> batchUpdateCategory(@Valid @RequestBody ArticleBatchCategoryDTO dto) {
        knowledgeArticleService.batchUpdateCategory(dto.getIds(), dto.getCategoryId());
        return Result.ok();
    }

    /**
     * 文章数据统计（管理员）：近30天浏览趋势、平均/累计阅读时长、各项计数
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('2')")
    public Result<ArticleStatsResponseDTO> getStats(@PathVariable String id) {
        return Result.ok(knowledgeArticleService.getStats(id));
    }

    // ==================== 前台（登录用户） ====================

    /**
     * 点赞 toggle（登录，article_like 唯一键 + like_count 同步）
     *
     * @return {liked: true/false, likeCount: 最新点赞数}
     */
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable String id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean liked = knowledgeArticleService.toggleLike(userId, id);
        // 查询最新点赞数返回给前端即时展示
        Map<String, Integer> counts = knowledgeArticleService.getCounts(id);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", counts.get("likeCount"));
        return Result.ok(result);
    }

    /**
     * 查询某文章已通过评论列表（前台，仅 status=1，分页）
     */
    @GetMapping("/{id}/comments")
    public Result<Map<String, Object>> listComments(@PathVariable String id,
                                                     @RequestParam(required = false) Long page,
                                                     @RequestParam(required = false) Long size) {
        IPage<ArticleCommentResponseDTO> commentPage = articleCommentService.listApproved(id, page, size);
        return Result.ok(Map.of("records", commentPage.getRecords(), "total", commentPage.getTotal()));
    }

    /**
     * 发表评论（登录，过敏感词过滤，默认待审核）
     */
    @PostMapping("/{id}/comment")
    public Result<Void> createComment(@PathVariable String id, @Valid @RequestBody ArticleCommentCreateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        articleCommentService.create(userId, id, dto.getContent());
        return Result.ok();
    }

    /**
     * 上报阅读时长（登录，累加到 user_browse_history.duration_seconds，30秒内重复忽略）
     */
    @PutMapping("/{id}/read-duration")
    public Result<Void> reportReadDuration(@PathVariable String id, @Valid @RequestBody ReadDurationDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        knowledgeArticleService.reportReadDuration(userId, id, dto.getSeconds());
        return Result.ok();
    }
}
