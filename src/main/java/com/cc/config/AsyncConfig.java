package com.cc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 *
 * <p>核心功能：定义两个独立的线程池，为 {@code @Async} 标注的方法提供线程池支持。</p>
 * <ul>
 *   <li>{@code taskExecutor}：8 线程，CPU 密集型任务专用（如计算处理、JSON 解析）</li>
 *   <li>{@code ioExecutor}：16 线程，IO 密集型任务专用（如文件上传、网络请求、邮件发送）</li>
 * </ul>
 *
 * <p>使用方式：在方法上标注 {@code @Async("ioExecutor")} 或 {@code @Async("taskExecutor")}
 * 即可路由到对应线程池；不指定名称时默认使用 {@code taskExecutor}。</p>
 *
 * <p>拒绝策略：CallerRunsPolicy——线程池满时由调用线程执行，避免任务丢失。</p>
 *
 * @author cc
 * @date 2026-09-09
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /** CPU 密集型线程池核心线程数（建议等于或略大于 CPU 核心数） */
    private static final int CPU_CORE_THREADS = 8;

    /** IO 密集型线程池核心线程数（IO 等待时间较长，可适当放大） */
    private static final int IO_CORE_THREADS = 16;

    /** 队列容量上限，防止无限堆积 */
    private static final int QUEUE_CAPACITY = 200;

    /** 空闲线程存活秒数 */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * CPU 密集型线程池
     *
     * <p>适用于计算密集任务：JSON 解析、加密计算、数据聚合等。
     * 线程数较少以避免线程切换开销。</p>
     *
     * @return 配置好的 ThreadPoolTaskExecutor
     */
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        return buildExecutor(CPU_CORE_THREADS, "task-exec-");
    }

    /**
     * IO 密集型线程池
     *
     * <p>适用于 IO 等待型任务：邮件发送、文件上传、外部 HTTP 调用、OSS 操作等。
     * 线程数较多以掩盖 IO 等待延迟。</p>
     *
     * @return 配置好的 ThreadPoolTaskExecutor
     */
    @Bean("ioExecutor")
    public ThreadPoolTaskExecutor ioExecutor() {
        return buildExecutor(IO_CORE_THREADS, "io-exec-");
    }

    /**
     * 构建线程池通用方法
     *
     * @param corePoolSize 核心线程数
     * @param threadNamePrefix 线程名前缀，便于日志排查
     * @return 配置好的 ThreadPoolTaskExecutor
     */
    private ThreadPoolTaskExecutor buildExecutor(int corePoolSize, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("异步线程池初始化完成, name={}, corePoolSize={}", threadNamePrefix, corePoolSize);
        return executor;
    }
}
