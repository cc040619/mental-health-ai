package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.ChapterSaveDTO;
import com.cc.dto.request.ChapterSortDTO;
import com.cc.dto.request.CommentReplyDTO;
import com.cc.dto.request.CourseReviewDTO;
import com.cc.dto.request.CourseSaveDTO;
import com.cc.dto.request.CourseStatusDTO;
import com.cc.dto.response.AdminChapterResponseDTO;
import com.cc.dto.response.AdminCourseDetailResponseDTO;
import com.cc.dto.response.CourseStatsResponseDTO;
import com.cc.service.CourseService;
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

import java.util.Map;

/**
 * 管理端课程内容管理控制器（管理员）
 *
 * <p>前缀 /api/admin/courses，全部接口仅管理员（user_type=2）可访问。
 * 课程状态机：DRAFT(0草稿) / PENDING(1待审核) / PUBLISHED(2已发布) /
 * REJECTED(3已驳回) / OFFLINE(4已下架)；状态硬校验在 Service 层。</p>
 *
 * <p>咨询师发布课程为后续扩展：本模块由管理员统一维护，
 * 状态机已兼容「讲师提交 → 管理员审核」场景。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    @Resource
    private CourseService courseService;

    // ==================== 课程 ====================

    /** 列表：keyword/category/status/page/size，聚合 章节数/总时长 */
    @GetMapping("/page")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") Long page,
                                            @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(courseService.adminPage(keyword, category, status, page, size));
    }

    /** 各状态课程计数（前端 Tab 数字） */
    @GetMapping("/status-count")
    @PreAuthorize("hasRole('2')")
    public Result<Map<Integer, Long>> statusCount() {
        return Result.ok(courseService.adminStatusCount());
    }

    /** 课程详情：基本信息 + 章节列表（按 sortOrder 升序） */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<AdminCourseDetailResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(courseService.adminDetail(id));
    }

    /** 创建课程：默认 DRAFT 草稿状态 */
    @PostMapping
    @PreAuthorize("hasRole('2')")
    public Result<Long> create(@Valid @RequestBody CourseSaveDTO dto) {
        return Result.ok(courseService.adminSave(dto));
    }

    /** 编辑基本信息：仅 DRAFT/REJECTED 可编辑 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CourseSaveDTO dto) {
        courseService.adminUpdate(id, dto);
        return Result.ok();
    }

    /** 提交审核：DRAFT/REJECTED → PENDING */
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('2')")
    public Result<Void> submit(@PathVariable Long id) {
        courseService.submitReview(id);
        return Result.ok();
    }

    /** 审核：PENDING → PUBLISHED / REJECTED */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('2')")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody CourseReviewDTO dto) {
        courseService.reviewCourse(id, dto.getApprove(), dto.getRejectReason());
        return Result.ok();
    }

    /** 上架/下架：PUBLISHED ↔ OFFLINE */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('2')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody CourseStatusDTO dto) {
        courseService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    // ==================== 章节 ====================

    /** 新增章节：sortOrder 自增，重算 totalDuration */
    @PostMapping("/{id}/chapters")
    @PreAuthorize("hasRole('2')")
    public Result<AdminChapterResponseDTO> addChapter(@PathVariable Long id,
                                                     @Valid @RequestBody ChapterSaveDTO dto) {
        return Result.ok(courseService.addChapter(id, dto));
    }

    /** 编辑章节：重算 totalDuration */
    @PutMapping("/chapters/{chapterId}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> updateChapter(@PathVariable Long chapterId,
                                     @Valid @RequestBody ChapterSaveDTO dto) {
        courseService.updateChapter(chapterId, dto);
        return Result.ok();
    }

    /** 删除章节：级联清理 progress，重算 totalDuration */
    @DeleteMapping("/chapters/{chapterId}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> deleteChapter(@PathVariable Long chapterId) {
        courseService.deleteChapter(chapterId);
        return Result.ok();
    }

    /** 批量重排章节：单事务 */
    @PutMapping("/chapters/sort")
    @PreAuthorize("hasRole('2')")
    public Result<Void> sortChapters(@Valid @RequestBody ChapterSortDTO dto) {
        courseService.sortChapters(dto);
        return Result.ok();
    }

    // ==================== 学习数据 ====================

    /** 学习人数 / 完成率 / 平均学习时长 / 章节热力图 */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('2')")
    public Result<CourseStatsResponseDTO> stats(@PathVariable Long id) {
        return Result.ok(courseService.getCourseStats(id));
    }

    // ==================== 评论 ====================

    /** 评论分页：courseId/keyword/page/size */
    @GetMapping("/comments")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> listComments(@RequestParam(required = false) Long courseId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") Long page,
                                                    @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(courseService.adminListComments(courseId, keyword, page, size));
    }

    /** 管理员回复评论 */
    @PutMapping("/comments/{id}/reply")
    @PreAuthorize("hasRole('2')")
    public Result<Void> replyComment(@PathVariable Long id, @Valid @RequestBody CommentReplyDTO dto) {
        courseService.replyComment(id, dto.getContent());
        return Result.ok();
    }

    /** 删除评论 */
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> deleteComment(@PathVariable Long id) {
        courseService.adminDeleteComment(id);
        return Result.ok();
    }
}
