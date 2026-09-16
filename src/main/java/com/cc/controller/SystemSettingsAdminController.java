package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.SettingsUpdateDTO;
import com.cc.service.SysConfigService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统设置管理端（按组查询 KV / 批量保存，key 白名单校验）
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('2')")
public class SystemSettingsAdminController {

    @Resource
    private SysConfigService sysConfigService;

    /**
     * 按组返回配置 KV（含 description）；group=BASIC|REGISTER|CONTENT|CRISIS，不传返回全部
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(value = "group", required = false) String group) {
        return Result.ok(sysConfigService.listByGroup(group));
    }

    /**
     * 批量保存：key 白名单 + 值校验，写后删缓存立即生效
     */
    @PutMapping
    public Result<Void> save(@Valid @RequestBody SettingsUpdateDTO dto) {
        sysConfigService.updateBatch(dto.getItems());
        return Result.ok(null);
    }
}
