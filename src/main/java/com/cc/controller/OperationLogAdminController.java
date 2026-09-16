package com.cc.controller;

import com.cc.common.Result;
import com.cc.service.OperationLogService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 操作日志管理端（操作人/类型/内容关键字/时间范围筛选，内容已脱敏直接展示）
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasRole('2')")
public class OperationLogAdminController {

    @Resource
    private OperationLogService operationLogService;

    /** 日志分页列表（size≤20，命中 idx_created） */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(value = "operator", required = false) String operator,
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", required = false) Long page,
            @RequestParam(value = "size", required = false) Long size) {
        return Result.ok(operationLogService.page(operator, actionType, keyword, startDate, endDate, page, size));
    }
}
