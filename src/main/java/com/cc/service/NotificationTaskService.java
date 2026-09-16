package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.entity.Appointment;
import com.cc.entity.Consultant;
import com.cc.entity.UserNotifySetting;
import com.cc.enums.AssessmentFrequency;
import com.cc.enums.NotificationType;
import com.cc.mapper.AppointmentMapper;
import com.cc.mapper.ConsultantMapper;
import com.cc.mapper.UserNotifySettingMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 消息通知定时任务
 *
 * <ul>
 *   <li>测评提醒：每日 20:00 扫描开启测评提醒的用户，按 DAILY/WEEKLY 频率生成通知</li>
 *   <li>预约提醒：每分钟扫描 1 小时内开始、已确认且未提醒过的预约，reminded_at 防重复</li>
 *   <li>未读数对账：每小时按 DB 重建 Redis 中不一致的计数</li>
 * </ul>
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class NotificationTaskService {

    /** 预约时间展示格式：M月d日 HH:mm */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    @Resource
    private UserNotifySettingMapper settingMapper;
    @Resource
    private AppointmentMapper appointmentMapper;
    @Resource
    private ConsultantMapper consultantMapper;
    @Resource
    private NotificationService notificationService;
    @Resource
    private AnnouncementService announcementService;

    /**
     * 定时发布公告：每分钟扫描到点的草稿公告（publish_at <= now），条件更新幂等
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public void publishDueAnnouncements() {
        try {
            int published = announcementService.publishDueAnnouncements();
            if (published > 0) {
                log.info("定时发布公告完成: {} 条", published);
            }
        } catch (Exception e) {
            log.warn("定时发布公告任务异常: {}", e.getMessage());
        }
    }

    /**
     * 测评提醒：每日 20:00。
     * DAILY 每天一条（当天已发则跳过）；WEEKLY 距上次提醒满 7 天（或从未提醒）；NONE 跳过。
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void remindAssessment() {
        List<UserNotifySetting> settings = settingMapper.selectList(
                new LambdaQueryWrapper<UserNotifySetting>()
                        .eq(UserNotifySetting::getAssessmentOn, 1));
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        for (UserNotifySetting setting : settings) {
            try {
                if (!shouldRemind(setting, now)) {
                    continue;
                }
                notificationService.createNotification(
                        setting.getUserId(),
                        NotificationType.ASSESSMENT,
                        "测评提醒",
                        "又到了定期心理测评时间，花 2 分钟了解自己的情绪状态吧",
                        "ASSESSMENT",
                        null);
                setting.setLastAssessmentRemindAt(now);
                settingMapper.updateById(setting);
                created++;
            } catch (Exception e) {
                log.warn("测评提醒处理失败: userId={}, err={}", setting.getUserId(), e.getMessage());
            }
        }
        log.info("测评提醒定时任务完成: 候选={}, 生成={}", settings.size(), created);
    }

    /**
     * 预约提前 1 小时提醒：每分钟扫描 [now, now+1h] 内开始、CONFIRMED、未提醒的预约。
     * 命中 idx_status_date（候选只取今明两天），内存中按 date+startTime 精确判定，
     * reminded_at 落库保证只提醒一次；咨询师姓名从 consultant 表关联获取。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 20_000L)
    public void remindAppointment() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        // 提醒窗口可能跨天（如 23:30 提醒次日 00:20），候选取今明两天
        List<Appointment> candidates = appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getStatus, "CONFIRMED")
                        .isNull(Appointment::getRemindedAt)
                        .between(Appointment::getAppointmentDate, today, today.plusDays(1)));
        for (Appointment appointment : candidates) {
            try {
                LocalDateTime start = LocalDateTime.of(
                        appointment.getAppointmentDate(), appointment.getStartTime());
                if (start.isBefore(now) || start.isAfter(now.plusHours(1))) {
                    continue;
                }
                String consultantName = "咨询师";
                Consultant consultant = consultantMapper.selectById(appointment.getConsultantId());
                if (consultant != null) {
                    consultantName = consultant.getName();
                }
                String timeText = start.format(TIME_FORMATTER);
                notificationService.createNotification(
                        appointment.getUserId(),
                        NotificationType.APPOINTMENT,
                        "预约提醒",
                        "您有一场咨询将在 1 小时后开始（" + timeText + " "
                                + consultantName + "），请提前准备",
                        "APPOINTMENT",
                        appointment.getId());
                appointment.setRemindedAt(now);
                appointmentMapper.updateById(appointment);
            } catch (Exception e) {
                log.warn("预约提醒处理失败: appointmentId={}, err={}", appointment.getId(), e.getMessage());
            }
        }
    }

    /**
     * 未读数对账：每小时一次，Redis 与 DB 不一致时以 DB 为准
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void reconcileUnread() {
        notificationService.reconcileUnreadCounts();
    }

    /** 按频率判定本次是否需要提醒（无状态日期规则，包内可见便于单测） */
    static boolean shouldRemind(UserNotifySetting setting, LocalDateTime now) {
        AssessmentFrequency frequency = AssessmentFrequency.fromCode(setting.getAssessmentFrequency());
        LocalDateTime last = setting.getLastAssessmentRemindAt();
        if (frequency == AssessmentFrequency.DAILY) {
            return last == null || last.toLocalDate().isBefore(now.toLocalDate());
        }
        if (frequency == AssessmentFrequency.WEEKLY) {
            // 距上次提醒满 7 天（含恰好 7 天）→ 提醒
            return last == null || !last.isAfter(now.minusDays(7));
        }
        return false;
    }
}
