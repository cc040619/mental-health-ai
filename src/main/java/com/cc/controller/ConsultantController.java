package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.response.ConsultantDetailResponseDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.service.ConsultantService;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 咨询师用户端控制器：列表/详情/可约时段/评价列表。
 * 全部接口需登录。
 *
 * @author cc
 * @date 2026-09-14
 */
@RestController
@RequestMapping("/api/consultant")
public class ConsultantController {

    @Resource
    private ConsultantService consultantService;

    /** 咨询师列表（擅长领域筛选 + 分页，走缓存，size≤20） */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String specialty,
                                            @RequestParam(required = false) Long page,
                                            @RequestParam(required = false) Long size) {
        return Result.ok(consultantService.list(specialty, page, size));
    }

    /** 咨询师详情 */
    @GetMapping("/{id}")
    public Result<ConsultantDetailResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(consultantService.detail(id));
    }

    /** 某日可约时段；date 为空返回未来 7 天聚合 */
    @GetMapping("/{id}/slots")
    public Result<List<SlotResponseDTO>> slots(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(consultantService.availableSlots(id, date));
    }

    /** 来访者评价列表（匿名展示） */
    @GetMapping("/{id}/reviews")
    public Result<Map<String, Object>> reviews(@PathVariable Long id,
                                               @RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size) {
        return Result.ok(consultantService.reviews(id, page, size));
    }
}
