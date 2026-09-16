package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.CrisisWordDTO;
import com.cc.dto.request.CrisisWordStatusDTO;
import com.cc.service.CrisisWordService;
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
 * 危机词库管理端（列表/新增/启停/删除/恢复默认）
 *
 * <p>安全护栏：删除/停用受"启用词不足 5 个拒绝"保护；恢复默认二次确认由前端弹窗承担。
 * 危机词不纳入 AI 回复过滤配置（4.12 底线不变）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/crisis-words")
@PreAuthorize("hasRole('2')")
public class CrisisWordAdminController {

    @Resource
    private CrisisWordService crisisWordService;

    /** 词库分页列表（keyword 模糊匹配） */
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                            @RequestParam(value = "page", required = false) Long page,
                                            @RequestParam(value = "size", required = false) Long size) {
        return Result.ok(crisisWordService.page(keyword, page, size));
    }

    /** 新增危机词（去重校验，reload 不重启生效） */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CrisisWordDTO dto) {
        crisisWordService.add(dto.getWord());
        return Result.ok(null);
    }

    /** 启停用（停用受护栏保护） */
    @PutMapping("/{id}")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody CrisisWordStatusDTO dto) {
        crisisWordService.updateStatus(id, dto.getStatus());
        return Result.ok(null);
    }

    /** 删除（启用词护栏：剩余启用词不足 5 拒绝） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        crisisWordService.delete(id);
        return Result.ok(null);
    }

    /** 恢复默认：清空词表 + 从 classpath crisis-words.txt 全量重建 */
    @PostMapping("/restore")
    public Result<Map<String, Object>> restore() {
        return Result.ok(crisisWordService.restoreDefault());
    }
}
