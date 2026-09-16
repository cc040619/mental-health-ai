package com.cc.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 预约模块定时任务：每分钟扫描已过结束时间的 CONFIRMED 预约自动置 COMPLETED。
 *
 * <p>与消息通知模块的「提前 1 小时提醒」任务互不干扰：
 * 提醒扫描 PENDING/CONFIRMED 且 reminded_at 为空，本任务只处理结束后的状态流转。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class AppointmentTaskService {

    @Resource
    private AppointmentService appointmentService;

    /**
     * 每分钟自动完成已结束的已确认预约
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public void autoCompleteDueAppointments() {
        try {
            appointmentService.autoCompleteDue();
        } catch (Exception e) {
            log.warn("预约自动完成任务异常: {}", e.getMessage());
        }
    }
}
