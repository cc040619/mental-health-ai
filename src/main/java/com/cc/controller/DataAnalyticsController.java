package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.response.DataAnalyticsResponseDTO;
import com.cc.service.DataAnalyticsService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析控制器
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/data-analytics")
public class DataAnalyticsController {

    @Resource
    private DataAnalyticsService dataAnalyticsService;

    // 获取数据分析概览（管理员后台仪表盘）
    @GetMapping("/overview")
    @PreAuthorize("hasRole('2')")
    public Result<DataAnalyticsResponseDTO> overview() {
        return Result.ok(dataAnalyticsService.overview());
    }
}
