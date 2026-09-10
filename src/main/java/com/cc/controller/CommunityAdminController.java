package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.ReportHandleDTO;
import com.cc.service.CommunityService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 社区后台管理控制器（管理员）
 *
 * <p>前缀 /api/admin，管理员权限在 Service 层 ensureAdmin 统一校验。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/api/admin")
public class CommunityAdminController {

    @Resource
    private CommunityService communityService;

    // 举报列表（按状态筛选：0待处理 1已下架 2已忽略）
    @GetMapping("/reports")
    public Result<Map<String, Object>> pageReports(@RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) Long page,
                                                   @RequestParam(required = false) Long size) {
        return Result.ok(communityService.pageReports(status, page, size));
    }

    // 处理举报：DOWN-下架内容  IGNORE-忽略
    @PutMapping("/reports/{id}")
    public Result<Void> handleReport(@PathVariable Long id,
                                     @Valid @RequestBody ReportHandleDTO dto) {
        communityService.handleReport(id, dto.getAction());
        return Result.ok();
    }
}
