package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.AnnouncementCreateDTO;
import com.cc.service.AnnouncementService;
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

import java.util.Map;

/**
 * 系统公告后台管理控制器（管理员）
 *
 * <p>前缀 /api/admin/announcements。状态机：创建一律草稿 → 发布 → 撤回；
 * 已发布须先撤回才能编辑/删除（内容安全约束，Service 层硬校验）。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@RestController
@RequestMapping("/api/admin/announcements")
@PreAuthorize("hasRole('2')")
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    // 公告列表（类型/状态/标题关键字筛选 + 阅读人数聚合，size≤20）
    @GetMapping
    public Result<Map<String, Object>> page(@RequestParam(required = false) String type,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long page,
                                            @RequestParam(required = false) Long size) {
        return Result.ok(announcementService.adminPage(type, status, keyword, page, size));
    }

    // 创建公告：一律存草稿（publishAt 未来时间由定时任务到点发布）
    @PostMapping
    public Result<Void> create(@Valid @RequestBody AnnouncementCreateDTO dto) {
        announcementService.create(dto);
        return Result.ok();
    }

    // 编辑公告：仅草稿/已撤回可编辑（已发布拒绝并提示先撤回）
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody AnnouncementCreateDTO dto) {
        announcementService.update(id, dto);
        return Result.ok();
    }

    // 立即发布（草稿/已撤回 → 已发布，撤回后重发场景）
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        announcementService.publish(id);
        return Result.ok();
    }

    // 撤回（已发布 → 已撤回，用户端立即不可见）
    @PutMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        announcementService.withdraw(id);
        return Result.ok();
    }

    // 删除公告：仅草稿/已撤回可删（级联清理阅读记录）
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.ok();
    }
}
