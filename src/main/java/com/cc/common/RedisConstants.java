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

    /** 消息通知业务模块 */
    public static final String MODULE_NOTIFY = "notify";

    /** 数据看板业务模块 */
    public static final String MODULE_DASHBOARD = "dashboard";

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

    // ==================== 消息通知模块 ====================

    /** 用户未读通知计数 Key 前缀，完整格式：MentalHealthAI:notify:unread:{userId} */
    public static final String NOTIFY_UNREAD_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_NOTIFY + SEPARATOR + "unread" + SEPARATOR;

    /** 已发布公告列表缓存 Key：MentalHealthAI:notify:announcement:list */
    public static final String NOTIFY_ANNOUNCEMENT_LIST_KEY =
            ROOT_NAMESPACE + SEPARATOR + MODULE_NOTIFY + SEPARATOR + "announcement" + SEPARATOR + "list";

    /** 公告列表缓存 TTL（分钟）：5 分钟兜底，发布/下架主动删缓存 */
    public static final long NOTIFY_ANNOUNCEMENT_LIST_TTL_MINUTES = 5L;

    /** 启用中的轮播图列表缓存 Key：MentalHealthAI:notify:banner:list */
    public static final String NOTIFY_BANNER_LIST_KEY =
            ROOT_NAMESPACE + SEPARATOR + MODULE_NOTIFY + SEPARATOR + "banner" + SEPARATOR + "list";

    /** 轮播图列表缓存 TTL（分钟）：5 分钟兜底，后台写操作主动删缓存 */
    public static final long NOTIFY_BANNER_LIST_TTL_MINUTES = 5L;

    // ==================== AI 模型配置模块 ====================

    /** AI 业务模块 */
    public static final String MODULE_AI = "ai";

    /** AI 配置 KV 整表缓存 Key：MentalHealthAI:ai:config（TTL 60s，写后删） */
    public static final String AI_CONFIG_CACHE_KEY =
            ROOT_NAMESPACE + SEPARATOR + MODULE_AI + SEPARATOR + "config";

    /** AI 提示词生效内容缓存前缀：MentalHealthAI:ai:prompt:{key}（TTL 60s，激活/保存后删） */
    public static final String AI_PROMPT_CACHE_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_AI + SEPARATOR + "prompt" + SEPARATOR;

    /** AI 配置/提示词缓存 TTL（秒）：60s 兜底，管理端写后主动删，配置准实时生效 */
    public static final long AI_CACHE_TTL_SECONDS = 60L;

    /** AI 实时测试限频 Key 前缀：MentalHealthAI:ai:test-limit:{adminId}（10 秒窗口防刷 token） */
    public static final String AI_TEST_LIMIT_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_AI + SEPARATOR + "test-limit" + SEPARATOR;

    /** AI 实时测试限频窗口（秒） */
    public static final long AI_TEST_LIMIT_SECONDS = 10L;

    /** MCP 出口限频计数 Key：MentalHealthAI:ai:mcp:rate（60s 固定窗口，所有 API Key 共用） */
    public static final String MCP_RATE_LIMIT_KEY =
            ROOT_NAMESPACE + SEPARATOR + MODULE_AI + SEPARATOR + "mcp" + SEPARATOR + "rate";

    /**
     * 构建用户未读通知计数 Key
     *
     * @param userId 用户ID
     * @return 完整 key，如 MentalHealthAI:notify:unread:1
     */
    public static String buildNotifyUnreadKey(Long userId) {
        return NOTIFY_UNREAD_PREFIX + userId;
    }

    // ==================== 预约咨询模块 ====================

    /** 预约咨询业务模块 */
    public static final String MODULE_CONSULTANT = "consultant";

    /** 咨询师列表缓存 Key 前缀：MentalHealthAI:consultant:list: */
    public static final String CONSULTANT_LIST_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_CONSULTANT + SEPARATOR + "list" + SEPARATOR;

    /** 咨询师详情缓存 Key 前缀：MentalHealthAI:consultant:detail: */
    public static final String CONSULTANT_DETAIL_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_CONSULTANT + SEPARATOR + "detail" + SEPARATOR;

    /** 咨询师列表/详情缓存 TTL（分钟）：评分/次数为冗余字段，5 分钟兜底，写操作主动删缓存 */
    public static final long CONSULTANT_CACHE_TTL_MINUTES = 5L;

    /**
     * 构建咨询师列表缓存 Key：MentalHealthAI:consultant:list:{specialty}:{page}:{size}
     *
     * @param specialty 擅长领域（null/空=全部）
     * @param page      页码
     * @param size      每页条数
     * @return 完整 key
     */
    public static String buildConsultantListKey(String specialty, long page, long size) {
        return CONSULTANT_LIST_PREFIX
                + (specialty == null || specialty.isBlank() ? "all" : specialty)
                + SEPARATOR + page + SEPARATOR + size;
    }

    /**
     * 构建咨询师详情缓存 Key：MentalHealthAI:consultant:detail:{id}
     *
     * @param consultantId 咨询师ID
     * @return 完整 key
     */
    public static String buildConsultantDetailKey(Long consultantId) {
        return CONSULTANT_DETAIL_PREFIX + consultantId;
    }

    // ==================== 数据看板模块 ====================

    /** 看板聚合结果缓存 Key 前缀：MentalHealthAI:dashboard: */
    public static final String DASHBOARD_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_DASHBOARD + SEPARATOR;

    /** 概览/核心指标缓存 TTL（分钟） */
    public static final long DASHBOARD_OVERVIEW_TTL_MINUTES = 5L;

    /** 含高危用户预警的测评统计缓存 TTL（分钟），时效要求最高 */
    public static final long DASHBOARD_RISK_TTL_MINUTES = 2L;

    /** 其余看板统计缓存 TTL（分钟） */
    public static final long DASHBOARD_DEFAULT_TTL_MINUTES = 10L;

    /**
     * 构建看板统计缓存 Key：MentalHealthAI:dashboard:{module}:{range}:{start}:{end}[{suffix}]
     *
     * @param module 统计模块（overview/user-trend/...）
     * @param range  范围编码（TODAY/7D/30D/CUSTOM）
     * @param start  起始日期 yyyy-MM-dd
     * @param end    结束日期 yyyy-MM-dd
     * @param suffix 追加维度（如 :day/:month），无则传 null
     * @return 完整 key
     */
    public static String buildDashboardKey(String module, String range, String start, String end, String suffix) {
        return DASHBOARD_PREFIX + module + SEPARATOR + range + SEPARATOR + start + SEPARATOR + end
                + (suffix == null || suffix.isBlank() ? "" : SEPARATOR + suffix);
    }

    /**
     * 构建与日期范围无关的看板缓存 Key：MentalHealthAI:dashboard:{module}
     */
    public static String buildDashboardKey(String module) {
        return DASHBOARD_PREFIX + module;
    }

    // ==================== 用户管理模块（管理端统计） ====================

    /** 用户统计聚合缓存 Key：MentalHealthAI:user:stats */
    public static final String USER_STATS_KEY =
            ROOT_NAMESPACE + SEPARATOR + MODULE_USER + SEPARATOR + "stats";

    /** 用户统计聚合缓存 TTL（分钟）：10 分钟兜底 */
    public static final long USER_STATS_TTL_MINUTES = 10L;

    // ==================== 系统设置模块 ====================

    /** 系统设置业务模块 */
    public static final String MODULE_SETTINGS = "settings";

    /** 系统配置分组缓存 Key 前缀：MentalHealthAI:settings:config:{group}（TTL 5min，写后删） */
    public static final String SYS_CONFIG_CACHE_PREFIX =
            ROOT_NAMESPACE + SEPARATOR + MODULE_SETTINGS + SEPARATOR + "config" + SEPARATOR;

    /** 系统配置缓存 TTL（分钟）：5 分钟兜底，管理端写后主动删 */
    public static final long SYS_CONFIG_CACHE_TTL_MINUTES = 5L;
}
