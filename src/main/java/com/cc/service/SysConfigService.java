package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.common.RedisConstants;
import com.cc.dto.request.SettingsUpdateDTO;
import com.cc.dto.response.CrisisConfigResponseDTO;
import com.cc.dto.response.PublicSettingsResponseDTO;
import com.cc.entity.SysConfig;
import com.cc.exception.BusinessException;
import com.cc.mapper.SysConfigMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置业务层：KV 读写（Redis 分组缓存 TTL 5min，写后删）+ key 白名单保存 +
 * 启动时播种默认配置 + 公开白名单出口
 *
 * <p>读路径全部走缓存，禁止每次请求查库；缓存/DB 异常 fail-open 回退默认值，
 * 保证注册/登录/公开页面不受影响。管理端保存 key 白名单校验（仅允许本类登记的 key），
 * 拒绝任意注入未知配置。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class SysConfigService {

    // ==================== 分组常量 ====================

    public static final String GROUP_BASIC = "BASIC";
    public static final String GROUP_REGISTER = "REGISTER";
    public static final String GROUP_CONTENT = "CONTENT";
    public static final String GROUP_CRISIS = "CRISIS";

    // ==================== 配置项 key（业务代码引用常量，避免魔法字符串） ====================

    public static final String KEY_SYSTEM_NAME = "SYSTEM_NAME";
    public static final String KEY_SYSTEM_LOGO = "SYSTEM_LOGO";
    public static final String KEY_ICP_NUMBER = "ICP_NUMBER";
    public static final String KEY_CUSTOMER_SERVICE = "CUSTOMER_SERVICE";
    public static final String KEY_SYSTEM_INTRO = "SYSTEM_INTRO";

    public static final String KEY_REGISTER_ENABLED = "REGISTER_ENABLED";
    public static final String KEY_REGISTER_REVIEW = "REGISTER_REVIEW";
    public static final String KEY_DEFAULT_USER_ROLE = "DEFAULT_USER_ROLE";

    public static final String KEY_COMMUNITY_POST_REVIEW = "COMMUNITY_POST_REVIEW";
    public static final String KEY_COMMUNITY_COMMENT_REVIEW = "COMMUNITY_COMMENT_REVIEW";

    public static final String KEY_HOTLINE_NUMBERS = "HOTLINE_NUMBERS";
    public static final String KEY_CRISIS_PROMPT_TEXT = "CRISIS_PROMPT_TEXT";

    /** 配置项登记表：key → {分组, 默认值, 描述}（LinkedHashMap 保持管理端展示顺序） */
    private record KeyDef(String group, String defaultValue, String description) {
    }

    private static final Map<String, KeyDef> KEY_DEFS = new LinkedHashMap<>();

    static {
        KEY_DEFS.put(KEY_SYSTEM_NAME, new KeyDef(GROUP_BASIC, "大学生心理健康AI助手", "系统名称"));
        KEY_DEFS.put(KEY_SYSTEM_LOGO, new KeyDef(GROUP_BASIC, "", "系统 Logo 图片 URL"));
        KEY_DEFS.put(KEY_ICP_NUMBER, new KeyDef(GROUP_BASIC, "", "ICP 备案号"));
        KEY_DEFS.put(KEY_CUSTOMER_SERVICE, new KeyDef(GROUP_BASIC, "", "客服联系方式"));
        KEY_DEFS.put(KEY_SYSTEM_INTRO, new KeyDef(GROUP_BASIC, "", "系统简介"));
        KEY_DEFS.put(KEY_REGISTER_ENABLED, new KeyDef(GROUP_REGISTER, "true", "开放注册（true/false）"));
        KEY_DEFS.put(KEY_REGISTER_REVIEW, new KeyDef(GROUP_REGISTER, "false", "注册需审核（true/false）"));
        KEY_DEFS.put(KEY_DEFAULT_USER_ROLE, new KeyDef(GROUP_REGISTER, "1", "注册默认角色（1-普通用户 2-管理员）"));
        KEY_DEFS.put(KEY_COMMUNITY_POST_REVIEW, new KeyDef(GROUP_CONTENT, "false", "社区发帖需审核（true/false）"));
        KEY_DEFS.put(KEY_COMMUNITY_COMMENT_REVIEW, new KeyDef(GROUP_CONTENT, "false", "社区评论需审核（true/false）"));
        KEY_DEFS.put(KEY_HOTLINE_NUMBERS, new KeyDef(GROUP_CRISIS,
                "[{\"name\":\"全国心理援助热线\",\"phone\":\"12356\",\"note\":\"2025 年上线的全国统一心理援助短号，24 小时接听\"},"
                        + "{\"name\":\"北京心理危机研究与干预中心\",\"phone\":\"010-82951332\",\"note\":\"国内最早的自杀研究与危机干预专业机构之一\"},"
                        + "{\"name\":\"希望24热线\",\"phone\":\"400-161-9995\",\"note\":\"24 小时心理危机干预热线，全国接入\"}]",
                "危机干预热线 JSON：[{name, phone, note?}]"));
        KEY_DEFS.put(KEY_CRISIS_PROMPT_TEXT, new KeyDef(GROUP_CRISIS,
                "如果你正在伤害自己，或有这样的想法，请先拨打下面任一条热线，有人正在等你。",
                "危机干预提示文案"));
    }

    /** 公开白名单 key（GET /api/settings/public 只暴露这 5 个展示类配置，不含任何开关） */
    private static final List<String> PUBLIC_KEYS = List.of(
            KEY_SYSTEM_NAME, KEY_SYSTEM_LOGO, KEY_ICP_NUMBER, KEY_CUSTOMER_SERVICE, KEY_SYSTEM_INTRO);

    /** 危机干预热线数量上限（防超长 JSON / 恶意刷配置） */
    private static final int HOTLINE_MAX_COUNT = 10;

    @Resource
    private SysConfigMapper sysConfigMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    // ==================== 启动播种 ====================

    /**
     * 启动时补齐缺失配置项（首次建表全量播种，后续新增 key 也自动补齐）；
     * 初始化失败仅告警不阻断启动——读取路径有默认值兜底
     */
    @PostConstruct
    public void initDefaults() {
        try {
            List<SysConfig> existing = sysConfigMapper.selectList(null);
            Map<String, SysConfig> existingMap = new HashMap<>();
            for (SysConfig row : existing) {
                existingMap.put(row.getConfigKey(), row);
            }
            int inserted = 0;
            for (Map.Entry<String, KeyDef> entry : KEY_DEFS.entrySet()) {
                if (existingMap.containsKey(entry.getKey())) {
                    continue;
                }
                KeyDef def = entry.getValue();
                sysConfigMapper.insert(SysConfig.builder()
                        .configKey(entry.getKey())
                        .configValue(def.defaultValue())
                        .groupName(def.group())
                        .description(def.description())
                        .build());
                inserted++;
            }
            if (inserted > 0) {
                log.info("系统配置播种完成: 新增 {} 项默认配置", inserted);
            }
        } catch (Exception e) {
            log.warn("系统配置初始化失败（fail-open，读取走默认值兜底）: {}", e.getMessage());
        }
    }

    // ==================== 读取（带缓存，fail-open 默认值兜底） ====================

    /**
     * 读单值：字符串（缺省返回登记默认值）
     */
    public String getString(String key) {
        String value = getGroupMap(groupOf(key)).get(key);
        return StringUtils.hasText(value) ? value : defaultOf(key);
    }

    /**
     * 读单值：布尔（仅 true 为 true，其余回退默认）
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getGroupMap(groupOf(key)).get(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim());
    }

    /**
     * 读某分组全部配置（缓存 JSON，TTL 5min）；DB 异常 fail-open 返回该组默认值
     */
    public Map<String, String> getGroupMap(String group) {
        String cacheKey = RedisConstants.SYS_CONFIG_CACHE_PREFIX + group;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<LinkedHashMap<String, String>>() {
                });
            }
        } catch (Exception e) {
            log.warn("系统配置缓存读取异常，回源DB: group={}, {}", group, e.getMessage());
        }
        Map<String, String> configMap = new LinkedHashMap<>();
        try {
            List<SysConfig> rows = sysConfigMapper.selectList(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getGroupName, group));
            for (SysConfig row : rows) {
                configMap.put(row.getConfigKey(), row.getConfigValue());
            }
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(configMap),
                    Duration.ofMinutes(RedisConstants.SYS_CONFIG_CACHE_TTL_MINUTES));
        } catch (Exception e) {
            log.warn("系统配置读取失败（fail-open，走默认值）: group={}, {}", group, e.getMessage());
            for (Map.Entry<String, KeyDef> entry : KEY_DEFS.entrySet()) {
                if (entry.getValue().group().equals(group)) {
                    configMap.putIfAbsent(entry.getKey(), entry.getValue().defaultValue());
                }
            }
        }
        return configMap;
    }

    /**
     * 管理端按组列出配置（含描述）；group 为空返回全部
     */
    public List<Map<String, Object>> listByGroup(String group) {
        List<String> groups = StringUtils.hasText(group) ? List.of(group) : List.of(
                GROUP_BASIC, GROUP_REGISTER, GROUP_CONTENT, GROUP_CRISIS);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String g : groups) {
            Map<String, String> configMap = getGroupMap(g);
            for (Map.Entry<String, KeyDef> entry : KEY_DEFS.entrySet()) {
                String key = entry.getKey();
                KeyDef def = entry.getValue();
                if (!def.group().equals(g)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("configKey", key);
                item.put("configValue", configMap.get(key));
                item.put("groupName", g);
                item.put("description", def.description());
                result.add(item);
            }
        }
        return result;
    }

    // ==================== 管理端保存（白名单 + 校验 + 写后删缓存） ====================

    /**
     * 批量保存：key 白名单（仅允许本类登记的 key，拒绝未知 key 防注入）、
     * 布尔/角色/热线 JSON 逐项校验，写后删全部分组缓存（立即生效）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(List<SettingsUpdateDTO.ConfigItem> items) {
        for (SettingsUpdateDTO.ConfigItem item : items) {
            String key = item.getConfigKey().trim();
            KeyDef def = KEY_DEFS.get(key);
            if (def == null) {
                throw new BusinessException("未知配置项，已拒绝保存: " + key);
            }
            String value = item.getConfigValue() == null ? "" : item.getConfigValue();
            validateValue(key, value);
            SysConfig existing = sysConfigMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
            if (existing != null) {
                existing.setConfigValue(value);
                sysConfigMapper.updateById(existing);
            } else {
                sysConfigMapper.insert(SysConfig.builder()
                        .configKey(key).configValue(value)
                        .groupName(def.group()).description(def.description())
                        .build());
            }
        }
        evictAllCache();
        log.info("保存系统配置: keys={}, operator=admin",
                items.stream().map(i -> i.getConfigKey()).toList());
    }

    // ==================== 公开白名单出口 ====================

    /**
     * 公开系统设置：只取 BASIC 组展示类 key，不含任何开关配置
     */
    public PublicSettingsResponseDTO getPublicSettings() {
        Map<String, String> basic = getGroupMap(GROUP_BASIC);
        return PublicSettingsResponseDTO.builder()
                .systemName(orDefault(basic.get(KEY_SYSTEM_NAME), KEY_SYSTEM_NAME))
                .systemLogo(orDefault(basic.get(KEY_SYSTEM_LOGO), KEY_SYSTEM_LOGO))
                .icpNumber(orDefault(basic.get(KEY_ICP_NUMBER), KEY_ICP_NUMBER))
                .customerService(orDefault(basic.get(KEY_CUSTOMER_SERVICE), KEY_CUSTOMER_SERVICE))
                .systemIntro(orDefault(basic.get(KEY_SYSTEM_INTRO), KEY_SYSTEM_INTRO))
                .build();
    }

    /**
     * 危机干预公开配置：热线数组 + 提示文案；JSON 解析失败回退默认热线
     */
    public CrisisConfigResponseDTO getCrisisConfig() {
        Map<String, String> crisis = getGroupMap(GROUP_CRISIS);
        List<CrisisConfigResponseDTO.Hotline> hotlines = parseHotlines(
                crisis.get(KEY_HOTLINE_NUMBERS), defaultOf(KEY_HOTLINE_NUMBERS));
        return CrisisConfigResponseDTO.builder()
                .hotlines(hotlines)
                .promptText(orDefault(crisis.get(KEY_CRISIS_PROMPT_TEXT), KEY_CRISIS_PROMPT_TEXT))
                .build();
    }

    /**
     * 热线 JSON 解析：结构 [{name, phone, note?}]，主值解析失败回退默认值
     */
    public List<CrisisConfigResponseDTO.Hotline> parseHotlines(String json, String fallbackJson) {
        List<CrisisConfigResponseDTO.Hotline> hotlines = doParseHotlines(json);
        return hotlines != null ? hotlines : doParseHotlines(fallbackJson);
    }

    private List<CrisisConfigResponseDTO.Hotline> doParseHotlines(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            List<CrisisConfigResponseDTO.Hotline> hotlines = new ArrayList<>();
            for (Map<String, Object> item : raw) {
                String name = item.get("name") == null ? null : String.valueOf(item.get("name"));
                String phone = item.get("phone") == null ? null : String.valueOf(item.get("phone"));
                if (!StringUtils.hasText(name) || !StringUtils.hasText(phone)) {
                    continue;
                }
                hotlines.add(CrisisConfigResponseDTO.Hotline.builder()
                        .name(name.trim())
                        .phone(phone.trim())
                        .note(item.get("note") == null ? null : String.valueOf(item.get("note")))
                        .build());
            }
            return hotlines;
        } catch (Exception e) {
            log.warn("热线 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 私有工具 ====================

    /**
     * 配置值校验：布尔项仅 true/false；默认角色仅 1/2；热线 JSON 结构合法且数量受限
     */
    private void validateValue(String key, String value) {
        switch (key) {
            case KEY_REGISTER_ENABLED, KEY_REGISTER_REVIEW,
                    KEY_COMMUNITY_POST_REVIEW, KEY_COMMUNITY_COMMENT_REVIEW -> {
                if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
                    throw new BusinessException("开关配置仅支持 true/false: " + key);
                }
            }
            case KEY_DEFAULT_USER_ROLE -> {
                if (!"1".equals(value.trim()) && !"2".equals(value.trim())) {
                    throw new BusinessException("默认角色仅支持 1（普通用户）/ 2（管理员）");
                }
            }
            case KEY_HOTLINE_NUMBERS -> {
                List<CrisisConfigResponseDTO.Hotline> hotlines = doParseHotlines(value);
                if (hotlines == null || hotlines.isEmpty()) {
                    throw new BusinessException("热线配置须为合法 JSON 数组且至少包含一条热线（name/phone 必填）");
                }
                if (hotlines.size() > HOTLINE_MAX_COUNT) {
                    throw new BusinessException("热线数量不能超过 " + HOTLINE_MAX_COUNT + " 条");
                }
            }
            default -> {
                // 其余文本类配置不限制内容（长度由 DTO @Size 约束）
            }
        }
    }

    private void evictAllCache() {
        try {
            for (String group : List.of(GROUP_BASIC, GROUP_REGISTER, GROUP_CONTENT, GROUP_CRISIS)) {
                stringRedisTemplate.delete(RedisConstants.SYS_CONFIG_CACHE_PREFIX + group);
            }
        } catch (Exception e) {
            log.warn("删除系统配置缓存失败（fail-open，等 5min 过期）: {}", e.getMessage());
        }
    }

    private String groupOf(String key) {
        KeyDef def = KEY_DEFS.get(key);
        return def != null ? def.group() : GROUP_BASIC;
    }

    private String defaultOf(String key) {
        KeyDef def = KEY_DEFS.get(key);
        return def != null ? def.defaultValue() : "";
    }

    private String orDefault(String value, String key) {
        return StringUtils.hasText(value) ? value : defaultOf(key);
    }
}
