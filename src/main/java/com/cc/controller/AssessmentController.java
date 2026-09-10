package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.AssessmentProgressDTO;
import com.cc.dto.request.AssessmentRecordPageQueryDTO;
import com.cc.dto.request.AssessmentStartDTO;
import com.cc.dto.request.AssessmentSubmitDTO;
import com.cc.dto.response.AssessmentRecordDetailDTO;
import com.cc.dto.response.AssessmentScaleDetailDTO;
import com.cc.dto.response.AssessmentScaleResponseDTO;
import com.cc.dto.response.AssessmentTrendResponseDTO;
import com.cc.service.AssessmentService;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
 * 心理测评控制器
 *
 * @author cc
 * @date 2026-09-08
 */
@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {

    @Resource
    private AssessmentService assessmentService;

    // 测评广场列表（类别筛选）
    @GetMapping("/scales")
    public Result<Map<String, Object>> listScales(String category, Long page, Long size) {
        return Result.ok(assessmentService.listScales(category, page, size));
    }

    // 量表详情（含题目与选项）
    @GetMapping("/scales/{id}")
    public Result<AssessmentScaleDetailDTO> getScaleDetail(@PathVariable Long id) {
        return Result.ok(assessmentService.getScaleDetail(id));
    }

    // 开始测评（创建/续答 IN_PROGRESS 记录）
    @PostMapping("/records/start")
    public Result<Map<String, Object>> startAssessment(@Valid @RequestBody AssessmentStartDTO startDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(assessmentService.startAssessment(userId, startDTO));
    }

    // 中途保存进度（退出续答）
    @PutMapping("/records/{id}/progress")
    public Result<Void> saveProgress(@PathVariable Long id, @Valid @RequestBody AssessmentProgressDTO progressDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        assessmentService.saveProgress(userId, id, progressDTO);
        return Result.ok();
    }

    // 提交作答：评分 → 报告
    @PostMapping("/records/{id}/submit")
    public Result<AssessmentRecordDetailDTO> submitAssessment(@PathVariable Long id,
                                                              @Valid @RequestBody AssessmentSubmitDTO submitDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(assessmentService.submitAssessment(userId, id, submitDTO));
    }

    // 记录详情（进度/报告，越权校验）
    @GetMapping("/records/{id}")
    public Result<AssessmentRecordDetailDTO> getRecordDetail(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(assessmentService.getRecordDetail(userId, id));
    }

    // 测评历史（分页）
    @GetMapping("/records")
    public Result<Map<String, Object>> listRecords(@Valid AssessmentRecordPageQueryDTO queryDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(assessmentService.listRecords(userId, queryDTO));
    }

    // 同量表历史趋势（折线图）
    @GetMapping("/records/trend")
    public Result<List<AssessmentTrendResponseDTO>> getTrend(Long scaleId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(assessmentService.getTrend(userId, scaleId));
    }
}
