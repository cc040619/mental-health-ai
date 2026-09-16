package com.cc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.common.Result;
import com.cc.dto.request.InterventionDTO;
import com.cc.dto.request.QuestionSaveDTO;
import com.cc.dto.request.ScaleQuestionsDTO;
import com.cc.dto.request.ScaleSaveDTO;
import com.cc.dto.response.AdminRecordResponseDTO;
import com.cc.dto.response.AdminReportDetailResponseDTO;
import com.cc.dto.response.AdminScaleDetailResponseDTO;
import com.cc.dto.response.AdminScaleListResponseDTO;
import com.cc.dto.response.AlertResponseDTO;
import com.cc.entity.AssessmentQuestion;
import com.cc.service.AssessmentQuestionService;
import com.cc.service.AssessmentService;
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

/**
 * 心理测评管理控制器（管理端）
 * <p>全部接口 requires hasRole('2')；测评记录/报告含用户心理健康敏感数据</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/assessments")
@PreAuthorize("hasRole('2')")
public class AdminAssessmentController {

    @Resource
    private AssessmentService assessmentService;

    @Resource
    private AssessmentQuestionService questionService;

    // ==================== 量表管理 ====================

    @GetMapping("/scales/page")
    public Result<IPage<AdminScaleListResponseDTO>> scalePage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(assessmentService.adminScalePage(keyword, category, enabled, page, size));
    }

    @PostMapping("/scales")
    public Result<Long> createScale(@Valid @RequestBody ScaleSaveDTO dto) {
        return Result.ok(assessmentService.createScale(dto));
    }

    @GetMapping("/scales/{id}")
    public Result<AdminScaleDetailResponseDTO> scaleDetail(@PathVariable Long id) {
        return Result.ok(assessmentService.adminScaleDetail(id));
    }

    @PutMapping("/scales/{id}")
    public Result<Void> updateScale(@PathVariable Long id, @Valid @RequestBody ScaleSaveDTO dto) {
        assessmentService.updateScale(id, dto);
        return Result.ok();
    }

    @PutMapping("/scales/{id}/questions")
    public Result<Void> saveQuestions(@PathVariable Long id, @Valid @RequestBody ScaleQuestionsDTO dto) {
        assessmentService.saveQuestions(id, dto);
        return Result.ok();
    }

    @PutMapping("/scales/{id}/enabled")
    public Result<Void> updateEnabled(@PathVariable Long id, @RequestParam Integer enabled) {
        assessmentService.updateEnabled(id, enabled);
        return Result.ok();
    }

    // ==================== 题目库 ====================

    @GetMapping("/questions/page")
    public Result<IPage<AssessmentQuestion>> questionPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(questionService.page(keyword, type, page, size));
    }

    @PostMapping("/questions")
    public Result<Long> createQuestion(@Valid @RequestBody QuestionSaveDTO dto) {
        return Result.ok(questionService.create(dto));
    }

    @PutMapping("/questions/{id}")
    public Result<Void> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionSaveDTO dto) {
        questionService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }

    // ==================== 测评记录 ====================

    @GetMapping("/records/page")
    public Result<IPage<AdminRecordResponseDTO>> recordPage(
            @RequestParam(required = false) Long scaleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(assessmentService.adminRecordPage(scaleId, keyword, level, startDate, endDate, page, size));
    }

    @GetMapping("/records/{id}")
    public Result<AdminReportDetailResponseDTO> recordDetail(@PathVariable Long id) {
        return Result.ok(assessmentService.adminRecordDetail(id));
    }

    // ==================== 高危预警 ====================

    @GetMapping("/alerts")
    public Result<IPage<AlertResponseDTO>> alerts(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(assessmentService.listAlerts(level, status, page, size));
    }

    @PutMapping("/alerts/{recordId}/intervention")
    public Result<Void> updateIntervention(@PathVariable Long recordId, @Valid @RequestBody InterventionDTO dto) {
        assessmentService.updateIntervention(recordId, dto);
        return Result.ok();
    }
}
