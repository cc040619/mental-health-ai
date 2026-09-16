package com.cc.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 数据看板缓存预热定时任务：每 10 分钟预计算 TODAY/7D/30D 各统计模块写入 Redis。
 *
 * <p>跨 Bean 调用保证 Spring AOP 生效；单模块异常由 DataAnalyticsService 内部消化，
 * 此处再兜底一次防止调度线程中断。CUSTOM 范围低频实时计算，不预热。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class DashboardWarmUpTaskService {

    @Resource
    private DataAnalyticsService dataAnalyticsService;

    /**
     * 每 10 分钟预热看板缓存；启动后首个整 10 分钟点执行，此前访问未命中则实时构建并回写
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void warmUpDashboard() {
        try {
            dataAnalyticsService.warmUpCache();
        } catch (Exception e) {
            log.warn("看板缓存预热任务异常: {}", e.getMessage());
        }
    }
}
