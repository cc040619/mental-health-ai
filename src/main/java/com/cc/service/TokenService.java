package com.cc.service;

import com.cc.common.RedisConstants;
import com.cc.config.JwtConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 管理服务，基于 Redis 维护用户登录 token 白名单
 *
 * <p>核心机制：</p>
 * <ul>
 *   <li>登录时将 token 写入 Redis，TTL 与 JWT 过期时间保持一致</li>
 *   <li>同一用户重复登录会覆盖旧 token，实现单会话控制</li>
 *   <li>登出时删除 Redis 中的 token 记录，使旧 token 立即失效</li>
 *   <li>JWT 过滤器在验签后校验 Redis 白名单，拦截已登出的 token</li>
 * </ul>
 *
 * @author cc
 * @date 2026-09-02
 */
@Slf4j
@Service
public class TokenService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtConfig jwtConfig;

    /**
     * 保存用户登录 token 到 Redis 白名单
     *
     * <p>同一用户重复登录时，新 token 会覆盖旧 token，
     * 旧 token 因不在白名单中而被拦截，实现单会话控制。</p>
     *
     * @param userId 用户ID
     * @param token  JWT token
     */
    public void saveLoginToken(Long userId, String token) {
        String key = RedisConstants.buildUserLoginTokenKey(userId);
        stringRedisTemplate.opsForValue().set(key, token, jwtConfig.getExpiration(), TimeUnit.MILLISECONDS);
    }

    /**
     * 校验 token 是否为当前有效的会话 token
     *
     * <p>校验逻辑：从 Redis 读取该用户当前存储的 token，与请求中的 token 比对。
     * 若 Redis 中无记录（已登出或已过期）或不一致（已被新登录覆盖），则判定无效。</p>
     *
     * @param userId 用户ID
     * @param token  请求中携带的 JWT token
     * @return true=有效，false=已失效（已登出 / 被替换 / Redis 异常）
     */
    public boolean isValidLoginToken(Long userId, String token) {
        try {
            String key = RedisConstants.buildUserLoginTokenKey(userId);
            String storedToken = stringRedisTemplate.opsForValue().get(key);
            return storedToken != null && storedToken.equals(token);
        } catch (Exception e) {
            log.error("校验登录 token 时 Redis 异常, userId={}", userId, e);
            // Redis 不可用时采用 fail-closed 策略，拒绝请求以保障安全
            return false;
        }
    }

    /**
     * 从 Redis 白名单移除用户登录 token（登出）
     *
     * @param userId 用户ID
     */
    public void removeLoginToken(Long userId) {
        String key = RedisConstants.buildUserLoginTokenKey(userId);
        stringRedisTemplate.delete(key);
    }
}
