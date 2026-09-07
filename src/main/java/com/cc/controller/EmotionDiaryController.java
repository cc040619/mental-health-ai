package com.cc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.common.Result;
import com.cc.dto.request.EmotionDiaryCreateDTO;
import com.cc.dto.request.EmotionDiaryPageQueryDTO;
import com.cc.dto.response.EmotionDiaryResponseDTO;
import com.cc.service.EmotionDiaryService;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 情绪日记控制器
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {

    @Resource
    private EmotionDiaryService emotionDiaryService;

    // 添加情绪日记（同一日期重复提交时覆盖更新）
    @PostMapping
    public Result<Map<String, Object>> add(@Valid @RequestBody EmotionDiaryCreateDTO createDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Long diaryId = emotionDiaryService.createDiary(userId, createDTO);
        return Result.ok(Map.of("id", diaryId));
    }

    // 后台分页查询日记列表（管理员）
    @GetMapping("/admin/page")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> pageForAdmin(@Valid EmotionDiaryPageQueryDTO queryDTO) {
        IPage<EmotionDiaryResponseDTO> page = emotionDiaryService.pageForAdmin(queryDTO);
        return Result.ok(Map.of("records", page.getRecords(), "total", page.getTotal()));
    }

    // 后台删除日记（管理员）
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> deleteForAdmin(@PathVariable Long id) {
        emotionDiaryService.deleteForAdmin(id);
        return Result.ok();
    }
}
