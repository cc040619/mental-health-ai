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

    /** 心理测评业务模块 */
    public static final String MODULE_ASSESSMENT = "assessment";

    /** 社区论坛业务模块 */
    public static final String MODULE_COMMUNITY = "community";

    // ==================== 具体功能 Key ====================

    /**
     * 用户登录 token 白名单 key 前缀
     * 完整格式：MentalHealthAI:user:login:token:{userId}
     */
    public static final String USER_LOGIN_TOKEN_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_USER + SEPARATOR + "login" + SEPARATOR + "token" + SEPARATOR;

    /**
     * 用户邮箱登录验证码 key 前缀
     * 完整格式：MentalHealthAI:user:login:email:code:{email}
     */
    public static final String USER_EMAIL_LOGIN_CODE_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_USER + SEPARATOR + "login" + SEPARATOR + "email" + SEPARATOR + "code" + SEPARATOR;

    /** 邮箱验证码有效期（分钟） */
    public static final long EMAIL_CODE_TTL_MINUTES = 5L;

    /** 邮箱验证码发送冷却时间（秒），防止频繁发送 */
    public static final long EMAIL_CODE_COOLDOWN_SECONDS = 60L;

    /**
     * 构建用户登录 token 的完整 Redis key
     *
     * @param userId 用户ID
     * @return 完整 key，如 MentalHealthAI:user:login:token:1
     */
    public static String buildUserLoginTokenKey(Long userId) {
        return USER_LOGIN_TOKEN_PREFIX + userId;
    }

    /**
     * 构建邮箱登录验证码的完整 Redis key
     *
     * @param email 邮箱地址
     * @return 完整 key，如 MentalHealthAI:user:login:email:code:xxx@qq.com
     */
    public static String buildEmailLoginCodeKey(String email) {
        return USER_EMAIL_LOGIN_CODE_PREFIX + email;
    }

    /**
     * 构建邮箱验证码发送冷却 key（用于防刷）
     *
     * @param email 邮箱地址
     * @return 完整 key，如 MentalHealthAI:user:login:email:cooldown:xxx@qq.com
     */
    public static String buildEmailLoginCooldownKey(String email) {
        return ROOT_NAMESPACE + SEPARATOR + MODULE_USER + SEPARATOR + "login" + SEPARATOR + "email" + SEPARATOR + "cooldown" + SEPARATOR + email;
    }

    // ==================== 心理测评模块 ====================

    /** 量表缓存 Key 前缀（按 scaleId）
     * 完整格式：MentalHealthAI:assessment:scale:cache:id:{scaleId} */
    public static final String ASSESSMENT_SCALE_CACHE_ID_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_ASSESSMENT + SEPARATOR + "scale" + SEPARATOR + "cache" + SEPARATOR + "id" + SEPARATOR;

    /** 量表缓存 Key 前缀（按 code）
     * 完整格式：MentalHealthAI:assessment:scale:cache:code:{code} */
    public static final String ASSESSMENT_SCALE_CACHE_CODE_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_ASSESSMENT + SEPARATOR + "scale" + SEPARATOR + "cache" + SEPARATOR + "code" + SEPARATOR;

    /** 量表列表缓存 Key 前缀（按类别）
     * 完整格式：MentalHealthAI:assessment:scale:list:{category} */
    public static final String ASSESSMENT_SCALE_LIST_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_ASSESSMENT + SEPARATOR + "scale" + SEPARATOR + "list" + SEPARATOR;

    /** 用户答题进度 Redis Key 前缀
     * 完整格式：MentalHealthAI:assessment:progress:{userId}:{recordId} */
    public static final String ASSESSMENT_PROGRESS_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_ASSESSMENT + SEPARATOR + "progress" + SEPARATOR;

    /** 量表缓存 TTL（小时）：量表数据静态，24 小时兜底 */
    public static final long ASSESSMENT_SCALE_CACHE_TTL_HOURS = 24L;

    /** 量表列表缓存 TTL（分钟）：1 小时 */
    public static final long ASSESSMENT_SCALE_LIST_TTL_MINUTES = 60L;

    /** 用户答题进度 Redis TTL（小时）：24 小时，覆盖正常作答周期 */
    public static final long ASSESSMENT_PROGRESS_TTL_HOURS = 24L;

    /**
     * 构建量表详情缓存 Key（按 scaleId）
     *
     * @param scaleId 量表ID
     * @return 完整 key
     */
    public static String buildAssessmentScaleCacheKey(Long scaleId) {
        return ASSESSMENT_SCALE_CACHE_ID_PREFIX + scaleId;
    }

    /**
     * 构建量表详情缓存 Key（按 code）
     *
     * @param code 量表编码
     * @return 完整 key
     */
    public static String buildAssessmentScaleCacheKeyByCode(String code) {
        return ASSESSMENT_SCALE_CACHE_CODE_PREFIX + code;
    }

    /**
     * 构建量表列表缓存 Key（按类别）
     *
     * @param category 类别（null/空=全部）
     * @return 完整 key
     */
    public static String buildAssessmentScaleListKey(String category) {
        return ASSESSMENT_SCALE_LIST_PREFIX + (category == null || category.isBlank() ? "all" : category);
    }

    /**
     * 构建用户答题进度 Redis Key
     *
     * @param userId   用户ID
     * @param recordId 作答记录ID
     * @return 完整 key
     */
    public static String buildAssessmentProgressKey(Long userId, Long recordId) {
        return ASSESSMENT_PROGRESS_PREFIX + userId + SEPARATOR + recordId;
    }

    // ==================== 社区论坛模块 ====================

    /** 帖子详情缓存 Key 前缀，完整格式：MentalHealthAI:community:post:{postId} */
    public static final String COMMUNITY_POST_CACHE_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_COMMUNITY + SEPARATOR + "post" + SEPARATOR;

    /** 热门帖子榜缓存 Key：MentalHealthAI:community:hot */
    public static final String COMMUNITY_HOT_KEY =
            ROOT_NAMESPACE + SEPARATOR + MODULE_COMMUNITY + SEPARATOR + "hot";

    /** 板块列表缓存 Key：MentalHealthAI:community:boards */
    public static final String COMMUNITY_BOARDS_KEY =
            ROOT_NAMESPACE + SEPARATOR + MODULE_COMMUNITY + SEPARATOR + "boards";

    /** 社区帖子详情缓存 TTL（分钟）：10 分钟兜底，写一致性靠延时双删 */
    public static final long COMMUNITY_POST_CACHE_TTL_MINUTES = 10L;

    /** 热门榜缓存 TTL（分钟）：5 分钟兜底 + 定时任务每 5 分钟重建 */
    public static final long COMMUNITY_HOT_TTL_MINUTES = 5L;

    /** 板块列表缓存 TTL（天）：板块为静态数据 */
    public static final long COMMUNITY_BOARDS_TTL_DAYS = 1L;

    /**
     * 构建帖子详情缓存 Key
     *
     * @param postId 帖子ID
     * @return 完整 key，如 MentalHealthAI:community:post:123
     */
    public static String buildCommunityPostCacheKey(Long postId) {
        return COMMUNITY_POST_CACHE_PREFIX + postId;
    }
}
