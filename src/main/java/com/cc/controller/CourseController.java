package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.CourseCommentCreateDTO;
import com.cc.dto.request.CourseProgressUpdateDTO;
import com.cc.dto.request.PracticeCheckinDTO;
import com.cc.dto.response.CourseDetailResponseDTO;
import com.cc.dto.response.CourseListResponseDTO;
import com.cc.dto.response.CourseMineResponseDTO;
import com.cc.dto.response.DailyPracticeResponseDTO;
import com.cc.service.CourseService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
 * 心理课程 / 冥想模块控制器
 * <p>全部接口需登录（SecurityConfig 默认拦截，PUBLIC_PATHS 不含本路径）
 *
 * @author cc
 * @date 2026-09-09
 */
@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Resource
    private CourseService courseService;

    // 课程广场（类别筛选 + 分页）
    @GetMapping("/list")
    public Result<Map<String, Object>> listCourses(String category, Long page, Long size) {
        return Result.ok(courseService.listCourses(category, page, size));
    }

    // 课程详情（介绍/章节含进度/完成度/收藏/评论数）
    @GetMapping("/{id}")
    public Result<CourseDetailResponseDTO> getCourseDetail(@PathVariable Long id) {
        return Result.ok(courseService.getCourseDetail(id));
    }

    // 收藏 toggle，返回最新状态
    @PostMapping("/{id}/favorite")
    public Result<Boolean> toggleFavorite(@PathVariable Long id) {
        return Result.ok(courseService.toggleFavorite(id));
    }

    // 我的收藏课程列表
    @GetMapping("/favorites")
    public Result<Map<String, Object>> listFavorites(Long page, Long size) {
        return Result.ok(courseService.listFavorites(page, size));
    }

    // 我的课程（已学习/学习中/已收藏）
    @GetMapping("/mine")
    public Result<CourseMineResponseDTO> listMine() {
        return Result.ok(courseService.listMine());
    }

    // 课程评论分页
    @GetMapping("/{id}/comments")
    public Result<Map<String, Object>> listComments(@PathVariable Long id, Long page, Long size) {
        return Result.ok(courseService.listComments(id, page, size));
    }

    // 发表评论
    @PostMapping("/{id}/comments")
    public Result<Object> createComment(@PathVariable Long id, @Valid @RequestBody CourseCommentCreateDTO dto) {
        return Result.ok(courseService.createComment(id, dto));
    }

    // 删除评论（仅本人或管理员）
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        courseService.deleteComment(id);
        return Result.ok();
    }

    // 上报学习进度（节流 upsert，首次学习 +1，达标置完成）
    @PutMapping("/progress")
    public Result<Map<String, Object>> updateProgress(@Valid @RequestBody CourseProgressUpdateDTO dto) {
        return Result.ok(courseService.updateProgress(dto));
    }

    // 今日推荐（每日一练）
    @GetMapping("/daily")
    public Result<DailyPracticeResponseDTO> getDailyPractice() {
        return Result.ok(courseService.getDailyPractice());
    }

    // 每日打卡（服务端校验当日已播 ≥60 秒）
    @PostMapping("/daily/checkin")
    public Result<Void> checkin(@Valid @RequestBody PracticeCheckinDTO dto) {
        courseService.checkin(dto);
        return Result.ok();
    }

    // 当月打卡日历 + 连续打卡天数
    @GetMapping("/checkins")
    public Result<Map<String, Object>> getCheckinCalendar(String month) {
        return Result.ok(courseService.getCheckinCalendar(month));
    }
}
