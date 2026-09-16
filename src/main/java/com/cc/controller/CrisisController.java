package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.CrisisPlanSaveDTO;
import com.cc.dto.response.CrisisPlanResponseDTO;
import com.cc.service.CrisisPlanService;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 危机干预控制器（前缀 /api/crisis，全部需登录）
 *
 * <p>热线号码纯前端常量（config/crisis.js），后端零参与、展示零延迟；
 * 本控制器仅提供个人安全计划读写。危机事件由 SSE 流式链路命中危机词时自动 @Async 记录，
 * 不对外暴露写入接口（避免滥用）。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@RestController
@RequestMapping("/api/crisis")
public class CrisisController {

    @Resource
    private CrisisPlanService crisisPlanService;

    /**
     * 获取我的安全计划（无记录返回空模板，不报错）
     */
    @GetMapping("/safety-plan")
    public Result<CrisisPlanResponseDTO> getMyPlan() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(crisisPlanService.getMyPlan(userId));
    }

    /**
     * 保存安全计划（仅本人可写，user_id 从登录态获取，越权不可写）
     */
    @PutMapping("/safety-plan")
    public Result<CrisisPlanResponseDTO> saveMyPlan(@Valid @RequestBody CrisisPlanSaveDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(crisisPlanService.saveMyPlan(userId, dto));
    }
}
