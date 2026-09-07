package com.cc.common;

/**
 * Redis Key 常量定义
 *
 * <p>统一命名空间规则：{@code MentalHealthAI:{业务模块}:{功能}:{标识}}</p>
 * <p>所有业务模块的 Redis Key 都应在此类中定义，避免散落在各处产生魔法字符串。</p>
 *
 * @author cc
 * @date 2026-09-02
 */
public final class RedisConstants {

    private RedisConstants() {
    }

    /** Redis 顶层命名空间 */
    public static final String ROOT_NAMESPACE = "MentalHealthAI";

    /** Key 分隔符 */
    public static final String SEPARATOR = ":";

    // ==================== 业务模块 ====================

    /** 用户业务模块 */
    public static final String MODULE_USER = "user";

    // ==================== 具体功能 Key ====================

    /**
     * 用户登录 token 白名单 key 前缀
     * 完整格式：MentalHealthAI:user:login:token:{userId}
     */
    public static final String USER_LOGIN_TOKEN_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_USER + SEPARATOR + "login" + SEPARATOR + "token" + SEPARATOR;

    /**
     * 构建用户登录 token 的完整 Redis key
     *
     * @param userId 用户ID
     * @return 完整 key，如 MentalHealthAI:user:login:token:1
     */
    public static String buildUserLoginTokenKey(Long userId) {
        return USER_LOGIN_TOKEN_PREFIX + userId;
    }
}
