package com.cc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 异步处理线程池配置：AI 流式对话（SSE/Flux）等异步请求使用线程池，
 * 替换默认 SimpleAsyncTaskExecutor（每请求新建线程、不复用、无上限，高并发下线程数失控）
 *
 * @author cc
 * @date 2026-09-15
 */
@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer {

    /**
     * ponytail: 池参数按毕设演示规模（几个并发流）设定；压测出现排队再调大
     */
    @Bean("mvcAsyncTaskExecutor")
    public ThreadPoolTaskExecutor mvcAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(mvcAsyncTaskExecutor());
    }
}
