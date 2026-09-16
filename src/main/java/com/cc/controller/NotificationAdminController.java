package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.NotificationPushDTO;
import com.cc.service.NotificationService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消息推送后台管理控制器（管理员）
 *
 * <p>推送=批量写站内通知（notification，related_type=PUSH、related_id=批次ID）+ 批次记录。
 * 送达率口径=落库即送达 100%（站内消息必然入库）；阅读率=批次内已读数/目标数，实时 JOIN 统计。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('2')")
public class NotificationAdminController {

    @Resource
    private NotificationService notificationService;

    // 批量推送：ALL 全体 / USERS 指定用户（≤500/次），同事务写批次，返回推送数
    @PostMapping("/push")
    public Result<Integer> push(@Valid @RequestBody NotificationPushDTO dto) {
        return Result.ok(notificationService.pushBatch(dto));
    }

    // 推送记录分页：标题/类型/目标数/阅读数/阅读率(实时统计)/时间
    @GetMapping("/pushes")
    public Result<Map<String, Object>> pushes(@RequestParam(required = false) Long page,
                                              @RequestParam(required = false) Long size) {
        return Result.ok(notificationService.pagePushes(page, size));
    }
}
