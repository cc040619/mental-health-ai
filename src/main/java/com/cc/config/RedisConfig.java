package com.cc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类，定义 RedisTemplate 的序列化策略
 *
 * <p>StringRedisTemplate 由 Spring Boot 自动装配，无需在此声明。
 * 此处配置的 RedisTemplate&lt;String, Object&gt; 供未来需要存储复杂对象的业务模块复用。</p>
 *
 * @author cc
 * @date 2026-09-02
 */
@Configuration
public class RedisConfig {

    /**
     * 通用 RedisTemplate：Key 采用 String 序列化，Value 采用 JSON 序列化
     *
     * @param connectionFactory Redis 连接工厂（自动注入）
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
