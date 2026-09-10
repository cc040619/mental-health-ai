package com.cc.config;

import com.cc.common.RedisConstants;
import com.cc.service.CommunityService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置：开启 Spring 调度
 *
 * <p>社区相关定时方法位于 {@link CommunityService}：
 * 每 5 分钟重建热门榜缓存、每小时清理无 TTL 残留 key。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduledConfig {

    @Resource
    private CommunityService communityService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 冷启动兜底：应用就绪后若热门榜缓存不存在则立即重建，避免首个请求穿透
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initHotCacheOnStartup() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.COMMUNITY_HOT_KEY);
            if (cached == null || cached.isBlank()) {
                communityService.rebuildHotCache();
                log.info("冷启动完成：社区热门榜缓存已重建");
            }
        } catch (Exception e) {
            // Redis 不可用不阻断启动，热门榜读路径会 fail-open 查库
            log.warn("冷启动热门榜重建失败（不影响启动）: {}", e.getMessage());
        }
    }
}
