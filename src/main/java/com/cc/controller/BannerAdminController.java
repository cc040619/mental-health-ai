package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.BannerSaveDTO;
import com.cc.dto.request.StatusToggleDTO;
import com.cc.entity.Banner;
import com.cc.service.BannerService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 轮播图后台管理控制器（管理员）：写操作后 Service 层删用户端缓存，首页即时生效
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/banners")
@PreAuthorize("hasRole('2')")
public class BannerAdminController {

    @Resource
    private BannerService bannerService;

    // 列表（含停用，按 sortOrder asc）
    @GetMapping
    public Result<List<Banner>> list() {
        return Result.ok(bannerService.listAdmin());
    }

    // 新增（imageUrl 为 OSS 上传产物，linkUrl 仅 http/https）
    @PostMapping
    public Result<Void> create(@Valid @RequestBody BannerSaveDTO dto) {
        bannerService.create(dto);
        return Result.ok();
    }

    // 编辑
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BannerSaveDTO dto) {
        bannerService.update(id, dto);
        return Result.ok();
    }

    // 启用 / 停用
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusToggleDTO dto) {
        bannerService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    // 删除
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.ok();
    }
}
