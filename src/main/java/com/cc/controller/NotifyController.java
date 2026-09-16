package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.NotifySettingUpdateDTO;
import com.cc.dto.response.AnnouncementResponseDTO;
import com.cc.dto.response.NotificationResponseDTO;
import com.cc.dto.response.NotifySettingResponseDTO;
import com.cc.dto.response.UnreadCountDTO;
import com.cc.service.AnnouncementService;
import com.cc.service.NotificationService;
import com.cc.service.NotifySettingService;
import com.cc.util.CurrentUserUtils;
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
 * 消息通知控制器（用户端）
 *
 * <p>前缀 /api/notify，全部接口需登录；归属校验在 Service 层完成。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    @Resource
    private NotificationService notificationService;
    @Resource
    private AnnouncementService announcementService;
    @Resource
    private NotifySettingService notifySettingService;

    // 通知列表（type 可选：ANNOUNCEMENT/APPOINTMENT/INTERACTION/ASSESSMENT，size≤50）
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String type,
                                            @RequestParam(required = false) Long page,
                                            @RequestParam(required = false) Long size) {
        return Result.ok(notificationService.listNotifications(type, page, size));
    }

    // 未读数（Redis O(1)）
    @GetMapping("/unread-count")
    public Result<UnreadCountDTO> unreadCount() {
        return Result.ok(new UnreadCountDTO(notificationService.getUnreadCount(CurrentUserUtils.getCurrentUserId())));
    }

    // 单条标记已读（校验归属本人）
    @PutMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    // 全部标记已读
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok();
    }

    // 公告列表（未读置顶，含 isRead）
    @GetMapping("/announcements")
    public Result<Map<String, Object>> announcements(@RequestParam(required = false) Long page,
                                                     @RequestParam(required = false) Long size) {
        return Result.ok(announcementService.listForUser(page, size));
    }

    // 公告标记已读
    @PutMapping("/announcements/{id}/read")
    public Result<Void> readAnnouncement(@PathVariable Long id) {
        announcementService.markRead(id);
        return Result.ok();
    }

    // 获取消息设置（无记录返回默认值并落库）
    @GetMapping("/settings")
    public Result<NotifySettingResponseDTO> getSettings() {
        return Result.ok(notifySettingService.getSettings());
    }

    // 更新消息设置
    @PutMapping("/settings")
    public Result<NotifySettingResponseDTO> updateSettings(@Valid @RequestBody NotifySettingUpdateDTO dto) {
        return Result.ok(notifySettingService.updateSettings(dto));
    }
}
