package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.AIservice.PromptManage;
import com.cc.common.RedisConstants;
import com.cc.dto.request.AiPromptSaveDTO;
import com.cc.entity.AiPromptTemplate;
import com.cc.exception.BusinessException;
import com.cc.mapper.AiPromptTemplateMapper;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
import java.util.Set;

/**
 * AI 提示词模板业务层：key 维度多版本管理（草稿→激活两步生效）+ 生效内容缓存
 *
 * <p>静态兜底机制：{@link #getActiveContentQuietly(String)} 供 PromptManage 静态方法调用
 * （静态 Holder 模式，保持 PromptManage 静态签名不变、调用方零改动）——
 * DB 无生效版本返回 null，PromptManage 回退静态常量，改造前后行为一致。
 * 缓存 TTL 60s 且无记录也缓存（NONE 标记），保证 AI 调用路径不每次查库。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class AiPromptService {

    /** 缓存"该 key 无生效版本"标记（与空串区分：空串=用户显式存了空内容） */
    private static final String CACHE_NONE = "__NONE__";

    /** 缓存值最大长度保护（提示词 ≤8000，缓存序列化安全） */
    private static final int SUMMARY_LENGTH = 100;

    /** 合法 prompt_key 白名单（与 PromptManage KEY_* 对应） */
    private static final Set<String> VALID_KEYS = Set.of(
            PromptManage.KEY_SYS_BASE, PromptManage.KEY_SCENE_EMOTION, PromptManage.KEY_SCENE_RELATIONSHIP,
            PromptManage.KEY_SCENE_ACADEMIC, PromptManage.KEY_SCENE_SLEEP, PromptManage.KEY_SCENE_OTHER,
            PromptManage.KEY_SUMMARY, PromptManage.KEY_EMOTION_ANALYSIS);

    @Resource
    private AiPromptTemplateMapper aiPromptTemplateMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AiConfigService aiConfigService;

    // ==================== 静态 Holder（PromptManage 兜底入口） ====================

    private static AiPromptService holder;

    @PostConstruct
    public void initHolder() {
        holder = this;
    }

    /**
     * PromptManage 静态兜底入口：读当前生效内容（缓存 60s），DB 无生效版本返回 null
     * 由调用方回退静态常量；任何异常静默返回 null，绝不阻塞 AI 调用
     */
    public static String getActiveContentQuietly(String promptKey) {
        if (holder == null) {
            return null;
        }
        try {
            return holder.getActiveContent(promptKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读某 key 当前生效内容：缓存命中直接返回；DB 无生效版本缓存 NONE 标记并返回 null
     */
    public String getActiveContent(String promptKey) {
        String cacheKey = RedisConstants.AI_PROMPT_CACHE_PREFIX + promptKey;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return CACHE_NONE.equals(cached) ? null : cached;
            }
        } catch (Exception e) {
            log.warn("提示词缓存读取异常，回源DB: key={}, {}", promptKey, e.getMessage());
        }
        AiPromptTemplate active = aiPromptTemplateMapper.selectOne(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getPromptKey, promptKey)
                        .eq(AiPromptTemplate::getStatus, AiPromptTemplate.STATUS_ACTIVE)
                        .last("limit 1"));
        String content = active != null ? active.getContent() : CACHE_NONE;
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, content,
                    Duration.ofSeconds(RedisConstants.AI_CACHE_TTL_SECONDS));
        } catch (Exception e) {
            log.warn("提示词缓存写入失败（fail-open）: key={}, {}", promptKey, e.getMessage());
        }
        return active != null ? active.getContent() : null;
    }

    // ==================== 管理端 ====================

    /**
     * 按 key 分组列表：key/名称/生效版本号与摘要/版本数（模板量小全量查后内存分组）
     */
    public List<Map<String, Object>> listGrouped() {
        List<AiPromptTemplate> all = aiPromptTemplateMapper.selectList(null);
        Map<String, List<AiPromptTemplate>> grouped = new LinkedHashMap<>();
        for (AiPromptTemplate row : all) {
            grouped.computeIfAbsent(row.getPromptKey(), k -> new ArrayList<>()).add(row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : PromptManage.KEY_ORDER) {
            Map<String, Object> item = new HashMap<>();
            item.put("promptKey", key);
            item.put("label", PromptManage.KEY_LABELS.get(key));
            List<AiPromptTemplate> versions = grouped.get(key);
            item.put("versionCount", versions != null ? versions.size() : 0);
            AiPromptTemplate active = versions != null ? versions.stream()
                    .filter(v -> v.getStatus() == AiPromptTemplate.STATUS_ACTIVE)
                    .findFirst().orElse(null) : null;
            if (active != null) {
                item.put("activeVersion", active.getVersion());
                item.put("activeId", active.getId());
                item.put("activeName", active.getName());
                item.put("activeSummary", summarize(active.getContent()));
            } else {
                item.put("activeVersion", null);
                item.put("activeSummary", null);
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 某 key 全部版本（内容只给摘要，全文走预览接口）
     */
    public List<Map<String, Object>> versions(String promptKey) {
        checkKey(promptKey);
        List<AiPromptTemplate> list = aiPromptTemplateMapper.selectList(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getPromptKey, promptKey)
                        .orderByDesc(AiPromptTemplate::getVersion));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiPromptTemplate row : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getId());
            item.put("version", row.getVersion());
            item.put("name", row.getName());
            item.put("status", row.getStatus());
            item.put("createdAt", row.getCreatedAt());
            item.put("summary", summarize(row.getContent()));
            result.add(item);
        }
        return result;
    }

    /**
     * 预览某版本全文
     */
    public Map<String, Object> preview(String promptKey, Integer version) {
        checkKey(promptKey);
        AiPromptTemplate row = aiPromptTemplateMapper.selectOne(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getPromptKey, promptKey)
                        .eq(AiPromptTemplate::getVersion, version));
        if (row == null) {
            throw new BusinessException("版本不存在");
        }
        Map<String, Object> item = new HashMap<>();
        item.put("id", row.getId());
        item.put("promptKey", row.getPromptKey());
        item.put("name", row.getName());
        item.put("content", row.getContent());
        item.put("version", row.getVersion());
        item.put("status", row.getStatus());
        item.put("createdAt", row.getCreatedAt());
        return item;
    }

    /**
     * 保存新版本：version 同 key 自增（唯一键兜底并发冲突）、一律草稿不生效——两步防误操作
     */
    public Map<String, Object> saveDraft(AiPromptSaveDTO dto) {
        String key = dto.getPromptKey().trim();
        checkKey(key);
        Integer maxVersion = aiPromptTemplateMapper.selectList(
                        new LambdaQueryWrapper<AiPromptTemplate>()
                                .eq(AiPromptTemplate::getPromptKey, key)
                                .orderByDesc(AiPromptTemplate::getVersion)
                                .last("limit 1"))
                .stream().findFirst().map(AiPromptTemplate::getVersion).orElse(0);
        int nextVersion = maxVersion + 1;
        try {
            AiPromptTemplate draft = AiPromptTemplate.builder()
                    .promptKey(key)
                    .name(dto.getName().trim())
                    .content(dto.getContent())
                    .version(nextVersion)
                    .status(AiPromptTemplate.STATUS_INACTIVE)
                    .build();
            aiPromptTemplateMapper.insert(draft);
            log.info("保存提示词草稿: key={}, version={}, operator=admin", key, nextVersion);
            Map<String, Object> result = new HashMap<>();
            result.put("version", nextVersion);
            result.put("message", "已保存为草稿 v" + nextVersion + "，需激活后生效");
            return result;
        } catch (DuplicateKeyException e) {
            // 双管理员并发同 key 建 v(n+1)：唯一键兜底，提示重试
            throw new BusinessException("版本号冲突，请刷新后重试");
        }
    }

    /**
     * 激活：该 key 旧生效行置 0 + 本行置 1（单事务），清缓存即时生效；
     * 已生效行重复激活幂等返回
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activate(Long id) {
        AiPromptTemplate target = aiPromptTemplateMapper.selectById(id);
        if (target == null) {
            throw new BusinessException("版本不存在");
        }
        Integer fromVersion = null;
        if (target.getStatus() != AiPromptTemplate.STATUS_ACTIVE) {
            AiPromptTemplate currentActive = aiPromptTemplateMapper.selectOne(
                    new LambdaQueryWrapper<AiPromptTemplate>()
                            .eq(AiPromptTemplate::getPromptKey, target.getPromptKey())
                            .eq(AiPromptTemplate::getStatus, AiPromptTemplate.STATUS_ACTIVE));
            fromVersion = currentActive != null ? currentActive.getVersion() : null;
            aiPromptTemplateMapper.update(null, new LambdaUpdateWrapper<AiPromptTemplate>()
                    .eq(AiPromptTemplate::getPromptKey, target.getPromptKey())
                    .eq(AiPromptTemplate::getStatus, AiPromptTemplate.STATUS_ACTIVE)
                    .set(AiPromptTemplate::getStatus, AiPromptTemplate.STATUS_INACTIVE));
            aiPromptTemplateMapper.update(null, new LambdaUpdateWrapper<AiPromptTemplate>()
                    .eq(AiPromptTemplate::getId, id)
                    .set(AiPromptTemplate::getStatus, AiPromptTemplate.STATUS_ACTIVE));
        }
        evictPromptCache(target.getPromptKey());
        log.info("激活提示词: key={}, version={}, operator=admin", target.getPromptKey(), target.getVersion());
        Map<String, Object> result = new HashMap<>();
        result.put("message", fromVersion != null
                ? "该 key 生效版本将从 v" + fromVersion + " 切到 v" + target.getVersion()
                : "该 key 生效版本已设为 v" + target.getVersion() + "（此前无生效版本）");
        return result;
    }

    /**
     * 恢复默认：以 PromptManage 静态常量内容生成新草稿版本（不自动激活）
     */
    public Map<String, Object> restoreDefault(String promptKey) {
        checkKey(promptKey);
        AiPromptSaveDTO dto = new AiPromptSaveDTO();
        dto.setPromptKey(promptKey);
        dto.setName("默认模板（恢复生成）");
        dto.setContent(PromptManage.getDefaultContent(promptKey));
        return saveDraft(dto);
    }

    /**
     * 提示词试跑：指定提示词内容 + 当前模型配置，无记忆单轮调用（供编辑预览）
     */
    public Map<String, Object> testRun(String content, String message) {
        return aiConfigService.test(content, message);
    }

    // ==================== 私有工具 ====================

    private void checkKey(String promptKey) {
        if (!VALID_KEYS.contains(promptKey)) {
            throw new BusinessException("非法的提示词 key");
        }
    }

    private void evictPromptCache(String promptKey) {
        try {
            stringRedisTemplate.delete(RedisConstants.AI_PROMPT_CACHE_PREFIX + promptKey);
        } catch (Exception e) {
            log.warn("删除提示词缓存失败（fail-open，等 60s 过期）: key={}, {}", promptKey, e.getMessage());
        }
    }

    private String summarize(String content) {
        if (content == null) {
            return "";
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() <= SUMMARY_LENGTH ? flat : flat.substring(0, SUMMARY_LENGTH) + "…";
    }
}
