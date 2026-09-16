package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.BlacklistDTO;
import com.cc.dto.request.BoardSaveDTO;
import com.cc.dto.request.PostFlagDTO;
import com.cc.dto.request.PostReviewDTO;
import com.cc.dto.request.ReportHandleDTO;
import com.cc.dto.request.SensitiveWordDTO;
import com.cc.dto.response.AdminBoardResponseDTO;
import com.cc.service.CommunityService;
import com.cc.service.SensitiveWordService;
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

import java.util.List;
import java.util.Map;

/**
 * 社区后台管理控制器（管理员）：帖子管理/审核/置顶/加精、评论审核/删除、黑名单、板块管理、
 * 举报处理、敏感词管理。管理员权限 Controller 层 @PreAuthorize 与 Service 层 ensureAdmin 双保险。
 *
 * @author cc
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('2')")
public class CommunityAdminController {

    @Resource
    private CommunityService communityService;
    @Resource
    private SensitiveWordService sensitiveWordService;

    // ==================== 帖子管理 ====================

    /** 帖子列表：板块/标题关键字/状态筛选（匿名帖作者显示「匿名用户」） */
    @GetMapping("/posts")
    public Result<Map<String, Object>> postPage(@RequestParam(required = false) String board,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) Long page,
                                                @RequestParam(required = false) Long size) {
        return Result.ok(communityService.adminPostPage(board, keyword, status, page, size));
    }

    /** 帖子审核：仅待审核可审；驳回原因必填 */
    @PutMapping("/posts/{id}/review")
    public Result<Void> reviewPost(@PathVariable Long id,
                                   @Valid @RequestBody PostReviewDTO dto) {
        communityService.reviewPost(id, dto.getApprove(), dto.getRejectReason());
        return Result.ok();
    }

    /** 帖子删除(软删)/下架：删除时评论联动软删 */
    @PutMapping("/posts/{id}/status")
    public Result<Void> updatePostStatus(@PathVariable Long id,
                                         @Valid @RequestBody PostFlagDTO dto) {
        communityService.updatePostStatus(id, dto.getValue());
        return Result.ok();
    }

    /** 置顶/取消置顶（value: 0/1） */
    @PutMapping("/posts/{id}/top")
    public Result<Void> updatePostTop(@PathVariable Long id,
                                      @Valid @RequestBody PostFlagDTO dto) {
        communityService.updatePostTop(id, dto.getValue());
        return Result.ok();
    }

    /** 加精/取消加精（value: 0/1） */
    @PutMapping("/posts/{id}/feature")
    public Result<Void> updatePostFeature(@PathVariable Long id,
                                          @Valid @RequestBody PostFlagDTO dto) {
        communityService.updatePostFeature(id, dto.getValue());
        return Result.ok();
    }

    // ==================== 评论管理 ====================

    /** 评论列表：帖子/内容关键字/状态筛选（含父评论摘要） */
    @GetMapping("/comments")
    public Result<Map<String, Object>> commentPage(@RequestParam(required = false) Long postId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) Long page,
                                                   @RequestParam(required = false) Long size) {
        return Result.ok(communityService.adminCommentPage(postId, keyword, status, page, size));
    }

    /** 评论审核：通过→正常；驳回→软删+count-1 */
    @PutMapping("/comments/{id}/review")
    public Result<Void> reviewComment(@PathVariable Long id,
                                      @Valid @RequestBody PostReviewDTO dto) {
        communityService.reviewComment(id, dto.getApprove());
        return Result.ok();
    }

    /** 删除评论（软删 + count-1） */
    @PutMapping("/comments/{id}/delete")
    public Result<Void> deleteComment(@PathVariable Long id) {
        communityService.softDeleteCommentAdmin(id);
        return Result.ok();
    }

    // ==================== 黑名单管理 ====================

    /** 黑名单分页 */
    @GetMapping("/blacklist")
    public Result<Map<String, Object>> blacklistPage(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Long page,
                                                     @RequestParam(required = false) Long size) {
        return Result.ok(communityService.blacklistPage(keyword, page, size));
    }

    /** 屏蔽用户（仅禁社区发帖/评论，不影响登录） */
    @PostMapping("/blacklist")
    public Result<Void> addBlacklist(@Valid @RequestBody BlacklistDTO dto) {
        communityService.addBlacklist(dto);
        return Result.ok();
    }

    /** 解除屏蔽 */
    @DeleteMapping("/blacklist/{userId}")
    public Result<Void> removeBlacklist(@PathVariable Long userId) {
        communityService.removeBlacklist(userId);
        return Result.ok();
    }

    // ==================== 板块管理 ====================

    /** 板块列表（含帖子数/版主昵称，全量含停用） */
    @GetMapping("/boards")
    public Result<List<AdminBoardResponseDTO>> boardList() {
        return Result.ok(communityService.adminBoardList());
    }

    /** 新增/编辑板块（编辑时 code 不可改） */
    @PostMapping("/boards")
    public Result<Void> saveBoard(@Valid @RequestBody BoardSaveDTO dto) {
        communityService.saveBoard(dto);
        return Result.ok();
    }

    /** 删除板块（有帖子拒绝） */
    @DeleteMapping("/boards/{id}")
    public Result<Void> deleteBoard(@PathVariable Long id) {
        communityService.deleteBoard(id);
        return Result.ok();
    }

    // ==================== 举报管理 ====================

    /** 举报列表（status: 0待处理 1已下架 2已忽略 3已警告） */
    @GetMapping("/reports")
    public Result<Map<String, Object>> pageReports(@RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) Long page,
                                                   @RequestParam(required = false) Long size) {
        return Result.ok(communityService.pageReports(status, page, size));
    }

    /** 处理举报：DELETE_POST-删除内容(联动软删)  WARN_USER-警告  IGNORE-忽略 */
    @PutMapping("/reports/{id}")
    public Result<Void> handleReport(@PathVariable Long id,
                                     @Valid @RequestBody ReportHandleDTO dto) {
        communityService.handleReport(id, dto.getAction());
        return Result.ok();
    }

    // ==================== 敏感词管理 ====================

    /** 词库分页：词/分类筛选 */
    @GetMapping("/sensitive-words")
    public Result<Map<String, Object>> sensitiveWordPage(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false) Long page,
                                                         @RequestParam(required = false) Long size) {
        return Result.ok(sensitiveWordService.page(keyword, category, page, size));
    }

    /** 新增敏感词（词唯一），写后重建词树不重启生效 */
    @PostMapping("/sensitive-words")
    public Result<Void> addSensitiveWord(@Valid @RequestBody SensitiveWordDTO dto) {
        sensitiveWordService.create(dto);
        return Result.ok();
    }

    /** 编辑敏感词（词/分类/启停用） */
    @PutMapping("/sensitive-words/{id}")
    public Result<Void> updateSensitiveWord(@PathVariable Long id,
                                            @Valid @RequestBody SensitiveWordDTO dto) {
        dto.setId(id);
        sensitiveWordService.update(dto);
        return Result.ok();
    }

    /** 删除敏感词 */
    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.ok();
    }

    /** 批量导入（≤1000/批，跳过重复，返回成功/重复/无效数） */
    @PostMapping("/sensitive-words/import")
    public Result<Map<String, Integer>> importSensitiveWords(@Valid @RequestBody com.cc.dto.request.ImportWordsDTO dto) {
        return Result.ok(sensitiveWordService.importWords(dto));
    }
}
