package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.common.RedisConstants;
import com.cc.dto.request.AnnouncementCreateDTO;
import com.cc.dto.response.AdminAnnouncementListResponseDTO;
import com.cc.dto.response.AnnouncementResponseDTO;
import com.cc.entity.Announcement;
import com.cc.entity.AnnouncementRead;
import com.cc.entity.User;
import com.cc.enums.AnnouncementStatus;
import com.cc.enums.AnnouncementType;
import com.cc.exception.BusinessException;
import com.cc.mapper.AnnouncementMapper;
import com.cc.mapper.AnnouncementReadMapper;
import com.cc.mapper.UserMapper;
import com.cc.util.CurrentUserUtils;
import com.cc.util.HtmlSanitizer;
import com.cc.util.SensitiveWordFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统公告业务层（三态版）
 *
 * <p>公告不写入 notification 全量广播，单独存 announcement + announcement_read（3.8 防爆表设计）；
 * 指定用户公告通过 target_type + target_user_ids 控制用户端可见性，同样不入通知表。
 * 状态机：DRAFT(0) → PUBLISHED(1) → WITHDRAWN(2)；已发布必须先撤回才能编辑/删除（内容安全）。
 * 全体公告列表缓存 Redis（TTL 5min），写操作与定时发布主动删缓存；
 * 指定用户公告不入公共缓存（每用户可见范围不同），实时查库（量小）。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class AnnouncementService {

    /** 管理端列表分页上限 */
    private static final long MAX_PAGE_SIZE = 20L;

    @Resource
    private AnnouncementMapper announcementMapper;
    @Resource
    private AnnouncementReadMapper announcementReadMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    // ==================== 用户端 ====================

    /**
     * 用户端公告列表：未读置顶 + 已发布时间倒序，携带当前用户 isRead。
     * 只可见 status=1 且（全体 或 当前用户在指定列表中）的公告。
     */
    public Map<String, Object> listForUser(Long page, Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;

        // 全体公告（公共缓存）+ 指定用户的公告（实时查库）
        List<Announcement> visible = new ArrayList<>(getPublishedAllList());
        visible.addAll(getPublishedForUser(userId));
        if (visible.isEmpty()) {
            return Map.of("records", List.of(), "total", 0L);
        }
        List<Long> ids = visible.stream().map(Announcement::getId).toList();
        Set<Long> readIds = announcementReadMapper.selectList(new LambdaQueryWrapper<AnnouncementRead>()
                        .eq(AnnouncementRead::getUserId, userId)
                        .in(AnnouncementRead::getAnnouncementId, ids))
                .stream().map(AnnouncementRead::getAnnouncementId).collect(Collectors.toSet());

        List<AnnouncementResponseDTO> all = visible.stream().map(a -> {
            AnnouncementResponseDTO dto = toDTO(a);
            dto.setIsRead(readIds.contains(a.getId()));
            return dto;
        }).sorted(Comparator
                .comparing(AnnouncementResponseDTO::getIsRead)
                .thenComparing(AnnouncementResponseDTO::getPublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int from = (int) Math.min((cur - 1) * sz, all.size());
        int to = (int) Math.min(from + sz, all.size());
        Map<String, Object> data = new HashMap<>();
        data.put("records", new ArrayList<>(all.subList(from, to)));
        data.put("total", (long) all.size());
        return data;
    }

    /**
     * 用户标记公告已读（唯一键防重，重复点击幂等）；范围外公告与不存在统一提示
     */
    public void markRead(Long announcementId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null || !canView(announcement, userId)) {
            throw new BusinessException("公告不存在");
        }
        announcementReadMapper.insertIgnore(announcementId, userId);
    }

    // ==================== 管理端 ====================

    /**
     * 管理端公告分页：类型/状态/关键字筛选 + 阅读人数一次聚合
     */
    public Map<String, Object> adminPage(String type, Integer status, String keyword, Long page, Long size) {
        ensureAdmin();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        IPage<AdminAnnouncementListResponseDTO> result = announcementMapper.selectAdminPage(
                new Page<>(cur, sz), type, status, kw);
        // 解析指定用户 ID 与昵称（仅 USERS 行，量小）
        for (AdminAnnouncementListResponseDTO dto : result.getRecords()) {
            List<Long> userIds = parseUserIds(dto.getTargetUserIdsJson());
            dto.setTargetUserIds(userIds);
            if (!userIds.isEmpty()) {
                dto.setTargetNames(userMapper.selectBatchIds(userIds).stream()
                        .map(u -> u.getNickname() != null && !u.getNickname().isBlank()
                                ? u.getNickname() : u.getUsername())
                        .collect(Collectors.joining("、")));
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 创建公告：一律存草稿（DRAFT），后续由「立即发布」或定时任务发布。
     * 内容过 HtmlSanitizer + 敏感词过滤（管理员发布同样拦截，防误发违规内容，与社区同词表）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(AnnouncementCreateDTO dto) {
        ensureAdmin();
        sensitiveWordFilter.checkSensitive(dto.getTitle());
        sensitiveWordFilter.checkSensitive(dto.getContent());
        Announcement announcement = Announcement.builder()
                .title(dto.getTitle().trim())
                .content(HtmlSanitizer.sanitize(dto.getContent()))
                .type(resolveType(dto.getType()))
                .targetType(resolveTargetType(dto.getTargetType()))
                .targetUserIds(serializeUserIds(dto))
                .publishAt(dto.getPublishAt())
                .status(AnnouncementStatus.DRAFT.getCode())
                .build();
        announcementMapper.insert(announcement);
        log.info("创建公告草稿: announcementId={}, operator=admin", announcement.getId());
    }

    /**
     * 编辑公告：仅 DRAFT/WITHDRAWN 可编辑（PUBLISHED 拒绝并提示先撤回——内容安全约束）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AnnouncementCreateDTO dto) {
        ensureAdmin();
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("公告不存在");
        }
        if (existing.getStatus() == AnnouncementStatus.PUBLISHED.getCode()) {
            throw new BusinessException("已发布公告需先撤回再编辑");
        }
        sensitiveWordFilter.checkSensitive(dto.getTitle());
        sensitiveWordFilter.checkSensitive(dto.getContent());
        // updateById 不更新 null 字段，publishAt 置空场景用 wrapper 显式 set
        announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .set(Announcement::getTitle, dto.getTitle().trim())
                .set(Announcement::getContent, HtmlSanitizer.sanitize(dto.getContent()))
                .set(Announcement::getType, resolveType(dto.getType()))
                .set(Announcement::getTargetType, resolveTargetType(dto.getTargetType()))
                .set(Announcement::getTargetUserIds, serializeUserIds(dto))
                .set(Announcement::getPublishAt, dto.getPublishAt()));
        log.info("编辑公告: announcementId={}, operator=admin", id);
    }

    /**
     * 立即发布：DRAFT/WITHDRAWN → PUBLISHED（撤回后重发场景）
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        ensureAdmin();
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("公告不存在");
        }
        if (AnnouncementStatus.fromCode(existing.getStatus()) == AnnouncementStatus.PUBLISHED) {
            throw new BusinessException("公告已是发布状态");
        }
        if ("USERS".equals(resolveTargetType(existing.getTargetType()))
                && parseUserIds(existing.getTargetUserIds()).isEmpty()) {
            throw new BusinessException("指定用户公告需先编辑并选择目标用户");
        }
        int updated = announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .ne(Announcement::getStatus, AnnouncementStatus.PUBLISHED.getCode())
                .set(Announcement::getStatus, AnnouncementStatus.PUBLISHED.getCode())
                .set(Announcement::getPublishedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("公告状态已变化，请刷新后重试");
        }
        evictListCache();
        log.info("发布公告: announcementId={}, operator=admin", id);
    }

    /**
     * 撤回公告：PUBLISHED → WITHDRAWN，用户端立即不可见（删缓存）
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        ensureAdmin();
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("公告不存在");
        }
        if (existing.getStatus() != AnnouncementStatus.PUBLISHED.getCode()) {
            throw new BusinessException("仅已发布公告可撤回");
        }
        int updated = announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .eq(Announcement::getStatus, AnnouncementStatus.PUBLISHED.getCode())
                .set(Announcement::getStatus, AnnouncementStatus.WITHDRAWN.getCode()));
        if (updated == 0) {
            throw new BusinessException("公告状态已变化，请刷新后重试");
        }
        evictListCache();
        log.info("撤回公告: announcementId={}, operator=admin", id);
    }

    /**
     * 删除公告：仅 DRAFT/WITHDRAWN 可删（已发布需先撤回）；同时清理阅读记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ensureAdmin();
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("公告不存在");
        }
        if (existing.getStatus() == AnnouncementStatus.PUBLISHED.getCode()) {
            throw new BusinessException("已发布公告需先撤回再删除");
        }
        announcementReadMapper.delete(new LambdaQueryWrapper<AnnouncementRead>()
                .eq(AnnouncementRead::getAnnouncementId, id));
        announcementMapper.deleteById(id);
        log.info("删除公告: announcementId={}, operator=admin", id);
    }

    /**
     * 定时发布扫描（定时任务每分钟调用）：到点草稿 → 已发布（幂等条件更新）。
     * 命中 idx_status_publish 索引；更新前状态仍为 DRAFT 才生效，并发安全。
     */
    public int publishDueAnnouncements() {
        List<Announcement> due = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, AnnouncementStatus.DRAFT.getCode())
                .isNotNull(Announcement::getPublishAt)
                .le(Announcement::getPublishAt, LocalDateTime.now()));
        int published = 0;
        for (Announcement announcement : due) {
            int updated = announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                    .eq(Announcement::getId, announcement.getId())
                    .eq(Announcement::getStatus, AnnouncementStatus.DRAFT.getCode())
                    .set(Announcement::getStatus, AnnouncementStatus.PUBLISHED.getCode())
                    .set(Announcement::getPublishedAt, LocalDateTime.now()));
            if (updated > 0) {
                published++;
            }
        }
        if (published > 0) {
            evictListCache();
        }
        return published;
    }

    // ==================== 缓存 ====================

    /**
     * 全体可见的已发布公告（缓存 TTL 5min，miss 回源 DB；指定用户公告不进公共缓存）
     */
    private List<Announcement> getPublishedAllList() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.NOTIFY_ANNOUNCEMENT_LIST_KEY);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<List<Announcement>>() {
                });
            }
        } catch (Exception e) {
            log.warn("公告列表缓存读取异常，回源DB: {}", e.getMessage());
        }
        List<Announcement> list = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, AnnouncementStatus.PUBLISHED.getCode())
                .eq(Announcement::getTargetType, "ALL")
                .orderByDesc(Announcement::getPublishedAt));
        // ponytail: 公告量级小（毕设），全量缓存+内存分页；公告量过百后改 DB JOIN 分页
        try {
            stringRedisTemplate.opsForValue().set(RedisConstants.NOTIFY_ANNOUNCEMENT_LIST_KEY,
                    objectMapper.writeValueAsString(list),
                    RedisConstants.NOTIFY_ANNOUNCEMENT_LIST_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("公告列表缓存写入失败（fail-open）: {}", e.getMessage());
        }
        return list;
    }

    /**
     * 当前用户可见的指定用户公告（实时查库不缓存：每用户可见范围不同）
     */
    private List<Announcement> getPublishedForUser(Long userId) {
        List<Announcement> list = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, AnnouncementStatus.PUBLISHED.getCode())
                .eq(Announcement::getTargetType, "USERS")
                .orderByDesc(Announcement::getPublishedAt));
        return list.stream()
                .filter(a -> parseUserIds(a.getTargetUserIds()).contains(userId))
                .toList();
    }

    /** 当前用户是否可见该公告（已发布 且 全体 或 在指定列表中） */
    private boolean canView(Announcement announcement, Long userId) {
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED.getCode()) {
            return false;
        }
        if (!"USERS".equals(announcement.getTargetType())) {
            return true;
        }
        return parseUserIds(announcement.getTargetUserIds()).contains(userId);
    }

    private void evictListCache() {
        try {
            stringRedisTemplate.delete(RedisConstants.NOTIFY_ANNOUNCEMENT_LIST_KEY);
        } catch (Exception e) {
            log.warn("删除公告列表缓存失败（fail-open）: {}", e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private void ensureAdmin() {
        if (!CurrentUserUtils.isAdmin()) {
            throw new BusinessException("A0301", "无管理员权限", null);
        }
    }

    private String resolveType(String type) {
        AnnouncementType t = AnnouncementType.fromCode(type);
        return t != null ? t.getCode() : AnnouncementType.SYSTEM.getCode();
    }

    private String resolveTargetType(String targetType) {
        return "USERS".equals(targetType) ? "USERS" : "ALL";
    }

    /** 校验并序列化指定用户列表：USERS 必填非空，ALL 置 null */
    private String serializeUserIds(AnnouncementCreateDTO dto) {
        if (!"USERS".equals(resolveTargetType(dto.getTargetType()))) {
            return null;
        }
        if (dto.getTargetUserIds() == null || dto.getTargetUserIds().isEmpty()) {
            throw new BusinessException("指定用户公告需选择目标用户");
        }
        List<Long> distinct = dto.getTargetUserIds().stream().distinct().toList();
        // 校验用户存在（≤500，一次批量查）
        List<Long> exists = userMapper.selectBatchIds(distinct).stream().map(User::getId).toList();
        if (exists.size() < distinct.size()) {
            throw new BusinessException("部分指定用户不存在，请重新选择");
        }
        try {
            return objectMapper.writeValueAsString(distinct);
        } catch (Exception e) {
            throw new BusinessException("目标用户序列化失败");
        }
    }

    /** JSON 字符串 → List&lt;Long&gt;（异常/空返回空列表） */
    private List<Long> parseUserIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private AnnouncementResponseDTO toDTO(Announcement a) {
        AnnouncementResponseDTO dto = new AnnouncementResponseDTO();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setContent(a.getContent());
        dto.setStatus(a.getStatus());
        dto.setPublishedAt(a.getPublishedAt());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
