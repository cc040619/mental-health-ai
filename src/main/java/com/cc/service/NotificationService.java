package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.common.RedisConstants;
import com.cc.dto.request.NotificationPushDTO;
import com.cc.dto.response.NotificationResponseDTO;
import com.cc.dto.response.PushRecordResponseDTO;
import com.cc.entity.Notification;
import com.cc.entity.NotificationPush;
import com.cc.entity.User;
import com.cc.entity.UserNotifySetting;
import com.cc.enums.NotificationType;
import com.cc.exception.BusinessException;
import com.cc.mapper.NotificationMapper;
import com.cc.mapper.NotificationPushMapper;
import com.cc.mapper.UserMapper;
import com.cc.mapper.UserNotifySettingMapper;
import com.cc.util.SensitiveWordFilter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息通知业务层
 *
 * <p>核心机制：</p>
 * <ul>
 *   <li>通知创建一律 {@link Async}（跨 Bean 调用生效），不阻塞点赞/评论/定时任务主流程；
 *       ponytail: 当前用 Spring 默认线程池，通知量大或需要削峰时升级 MQ</li>
 *   <li>未读数走 Redis 计数（INCR/DECR），冷启动无 key 时从 DB COUNT 重建；每小时对账兜底</li>
 *   <li>互动通知按「接收者+类型+关联帖子+内容完全一致」去重：内容由 动作+互动者+目标 确定性拼成，
 *       天然覆盖「同一人点赞→取消→再点赞」的重复，又不会误伤不同互动者/不同动作</li>
 *   <li>用户关闭对应类型开关后不再创建通知；无设置记录时按默认全开处理</li>
 * </ul>
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class NotificationService {

    /** 列表分页上限，防止超大 size 拖库 */
    private static final long MAX_PAGE_SIZE = 50L;

    @Resource
    private NotificationMapper notificationMapper;
    @Resource
    private NotificationPushMapper notificationPushMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserNotifySettingMapper settingMapper;
    @Resource
    private SensitiveWordFilter sensitiveWordFilter;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 创建通知（异步）：预约确认/取消/提醒、测评提醒等非互动场景使用。
     * 必须由其他 Bean 调用，同类自调用 {@code @Async} 不生效。
     *
     * @param userId      接收者ID
     * @param type        通知类型 {@link NotificationType}
     * @param title       标题
     * @param content     内容（不打印日志）
     * @param relatedType 关联类型 POST/APPOINTMENT/ASSESSMENT，可空
     * @param relatedId   关联业务ID，可空
     */
    @Async("taskExecutor")
    public void createNotification(Long userId, NotificationType type, String title, String content,
                                   String relatedType, Long relatedId) {
        if (userId == null || type == null) {
            return;
        }
        if (!isTypeEnabled(userId, type)) {
            log.info("用户已关闭该类通知，跳过创建: userId={}, type={}", userId, type.getCode());
            return;
        }
        persistAndIncr(Notification.builder()
                .userId(userId)
                .type(type.getCode())
                .title(title)
                .content(content)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .isRead(0)
                .build());
    }

    /**
     * 创建互动通知（异步）：社区点赞/评论/楼中楼回复使用，带去重与互动开关校验。
     *
     * @param receiverId 接收者ID（帖子作者或被回复评论作者）
     * @param title      标题（点赞提醒/评论提醒/回复提醒）
     * @param content    完整通知内容（匿名帖已脱敏为「有用户」），同时作为去重指纹
     * @param postId     关联帖子ID（related_id 统一指向帖子）
     */
    @Async("taskExecutor")
    public void createInteractionNotification(Long receiverId, String title, String content, Long postId) {
        if (receiverId == null) {
            return;
        }
        if (!isTypeEnabled(receiverId, NotificationType.INTERACTION)) {
            log.info("用户已关闭互动通知，跳过创建: receiverId={}", receiverId);
            return;
        }
        // 去重：同一接收者存在同帖子同内容的未读互动通知则不重复创建
        Long exists = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, receiverId)
                .eq(Notification::getType, NotificationType.INTERACTION.getCode())
                .eq(Notification::getRelatedId, postId)
                .eq(Notification::getIsRead, 0)
                .eq(Notification::getContent, content));
        if (exists != null && exists > 0) {
            log.info("未读互动通知已存在，去重跳过: receiverId={}, postId={}", receiverId, postId);
            return;
        }
        persistAndIncr(Notification.builder()
                .userId(receiverId)
                .type(NotificationType.INTERACTION.getCode())
                .title(title)
                .content(content)
                .relatedType("POST")
                .relatedId(postId)
                .isRead(0)
                .build());
    }

    /**
     * 通知列表（本人）：按创建时间倒序，可选类型过滤，命中索引 idx_user_read_created
     */
    public Map<String, Object> listNotifications(String type, Long page, Long size) {
        Long userId = currentUserId();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        if (type != null && !type.isBlank()) {
            NotificationType t = NotificationType.fromCode(type);
            if (t == null) {
                throw new BusinessException("通知类型不正确");
            }
            wrapper.eq(Notification::getType, t.getCode());
        }
        IPage<Notification> result = notificationMapper.selectPage(new Page<>(cur, sz), wrapper);
        List<NotificationResponseDTO> records = result.getRecords().stream().map(this::toDTO).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 未读数：Redis O(1)；key 不存在（冷启动/过期）时从 DB COUNT 重建；
     * Redis 异常 fail-open 直接查库
     */
    public long getUnreadCount(Long userId) {
        String key = RedisConstants.buildNotifyUnreadKey(userId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached);
            }
            long dbCount = countUnreadFromDb(userId);
            stringRedisTemplate.opsForValue().set(key, String.valueOf(dbCount));
            return dbCount;
        } catch (Exception e) {
            log.warn("未读数读 Redis 失败，回源 DB: userId={}, err={}", userId, e.getMessage());
            return countUnreadFromDb(userId);
        }
    }

    /**
     * 单条标记已读：校验归属本人，未读→已读并 Redis DECR（不低于 0）
     */
    public void markRead(Long id) {
        Long userId = currentUserId();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !userId.equals(notification.getUserId())) {
            // 越权与不存在统一提示，不泄露通知是否存在
            throw new BusinessException("A0301", "通知不存在或无权操作", null);
        }
        if (Integer.valueOf(1).equals(notification.getIsRead())) {
            return;
        }
        int updated = notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
        if (updated > 0) {
            decrUnread(userId);
        }
    }

    /**
     * 全部标记已读：单条批量 UPDATE，随后 Redis 计数直接置 0（不逐条 DECR）
     */
    public void markAllRead() {
        Long userId = currentUserId();
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
        try {
            stringRedisTemplate.opsForValue().set(RedisConstants.buildNotifyUnreadKey(userId), "0");
        } catch (Exception e) {
            log.warn("全部已读后重置 Redis 未读数失败（对账兜底）: userId={}, err={}", userId, e.getMessage());
        }
    }

    /**
     * 每小时对账：扫描 Redis 中的未读计数 key，与 DB 未读数不一致时以 DB 为准重建。
     * 冷启动丢失的 key 不主动扫描重建，由用户读接口懒加载，避免全量用户扫描。
     */
    public void reconcileUnreadCounts() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(RedisConstants.NOTIFY_UNREAD_PREFIX + "*")
                .count(200).build();
        int fixed = 0;
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long userId = parseUserId(key);
                if (userId == null) {
                    continue;
                }
                String cached = stringRedisTemplate.opsForValue().get(key);
                long dbCount = countUnreadFromDb(userId);
                if (cached == null || Long.parseLong(cached) != dbCount) {
                    stringRedisTemplate.opsForValue().set(key, String.valueOf(dbCount));
                    fixed++;
                }
            }
        } catch (Exception e) {
            log.warn("未读数对账异常: {}", e.getMessage());
        }
        if (fixed > 0) {
            log.info("未读数对账完成，重建用户数: {}", fixed);
        }
    }

    // ==================== 管理端：消息推送 ====================

    /**
     * 批量推送站内通知（管理员）：ALL 全体 / USERS 指定用户（≤500），同事务写批次记录。
     *
     * <p>与 {@link #createNotification} 的差异：管理员强推不走用户通知开关，也不走 @Async
     * （批量语义需与批次记录同事务，异步单插无法保证一致性）；送达率口径=落库即送达 100%。</p>
     *
     * <p>ponytail: 循环单插 + 循环 INCR 未读数，用户量过万时改 foreach 批量 insert + Redis pipeline/MQ</p>
     *
     * @return 实际推送用户数
     */
    @Transactional(rollbackFor = Exception.class)
    public int pushBatch(NotificationPushDTO dto) {
        // 管理员推送同样过滤敏感词，防误发违规内容（与社区同词表）
        sensitiveWordFilter.checkSensitive(dto.getTitle());
        sensitiveWordFilter.checkSensitive(dto.getContent());
        NotificationType type = NotificationType.fromCode(dto.getType());
        if (type == null) {
            throw new BusinessException("推送类型不正确");
        }
        String targetType = "USERS".equals(dto.getTargetType()) ? "USERS" : "ALL";
        List<Long> userIds;
        if ("USERS".equals(targetType)) {
            if (dto.getTargetUserIds() == null || dto.getTargetUserIds().isEmpty()) {
                throw new BusinessException("指定用户推送需选择目标用户");
            }
            List<Long> distinct = dto.getTargetUserIds().stream().distinct().toList();
            List<Long> exists = userMapper.selectBatchIds(distinct).stream().map(User::getId).toList();
            if (exists.size() < distinct.size()) {
                throw new BusinessException("部分指定用户不存在，请重新选择");
            }
            userIds = distinct;
        } else {
            // 全体用户（仅取 ID 列）
            userIds = userMapper.selectList(new LambdaQueryWrapper<User>().select(User::getId))
                    .stream().map(User::getId).toList();
            if (userIds.isEmpty()) {
                throw new BusinessException("暂无用户可推送");
            }
        }

        NotificationPush push = NotificationPush.builder()
                .type(type.getCode())
                .title(dto.getTitle().trim())
                .content(dto.getContent())
                .targetType(targetType)
                .targetCount(userIds.size())
                .build();
        notificationPushMapper.insert(push);

        // 批量落站内通知：related_type=PUSH、related_id=批次ID，供推送记录阅读率实时统计
        for (Long userId : userIds) {
            notificationMapper.insert(Notification.builder()
                    .userId(userId)
                    .type(type.getCode())
                    .title(dto.getTitle().trim())
                    .content(dto.getContent())
                    .relatedType("PUSH")
                    .relatedId(push.getId())
                    .isRead(0)
                    .build());
            try {
                stringRedisTemplate.opsForValue().increment(RedisConstants.buildNotifyUnreadKey(userId));
            } catch (Exception e) {
                log.warn("推送未读数 INCR 失败（每小时对账兜底）: userId={}", userId);
            }
        }
        log.info("消息推送完成: pushId={}, type={}, target={}, count={}",
                push.getId(), type.getCode(), targetType, userIds.size());
        return userIds.size();
    }

    /**
     * 推送记录分页：LEFT JOIN notification 一次聚合已读数（实时统计，无冗余字段），禁止 N+1
     */
    public Map<String, Object> pagePushes(Long page, Long size) {
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        IPage<PushRecordResponseDTO> result = notificationPushMapper.selectPushPage(new Page<>(cur, sz));
        for (PushRecordResponseDTO dto : result.getRecords()) {
            long target = dto.getTargetCount() == null ? 0 : dto.getTargetCount();
            long read = dto.getReadCount() == null ? 0 : dto.getReadCount();
            dto.setReadRate(target == 0 ? 0 : (int) Math.min(100, read * 100 / target));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    // ==================== 内部方法 ====================

    /** 落库 + 未读数 INCR；Redis 失败不影响落库（每小时对账兜底） */
    private void persistAndIncr(Notification notification) {
        notificationMapper.insert(notification);
        try {
            stringRedisTemplate.opsForValue().increment(RedisConstants.buildNotifyUnreadKey(notification.getUserId()));
        } catch (Exception e) {
            log.warn("未读数 INCR 失败（对账兜底）: userId={}, type={}, err={}",
                    notification.getUserId(), notification.getType(), e.getMessage());
        }
        log.info("创建通知: userId={}, type={}, relatedId={}",
                notification.getUserId(), notification.getType(), notification.getRelatedId());
    }

    /** DECR 并保证不低于 0（key 缺失等场景会先得到 -1，立即纠正为 0） */
    private void decrUnread(Long userId) {
        try {
            Long value = stringRedisTemplate.opsForValue().decrement(RedisConstants.buildNotifyUnreadKey(userId));
            if (value != null && value < 0) {
                stringRedisTemplate.opsForValue().set(RedisConstants.buildNotifyUnreadKey(userId), "0");
            }
        } catch (Exception e) {
            log.warn("未读数 DECR 失败（对账兜底）: userId={}, err={}", userId, e.getMessage());
        }
    }

    private long countUnreadFromDb(Long userId) {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return count == null ? 0L : count;
    }

    /** 该类型通知是否开启；无设置记录按默认全开 */
    private boolean isTypeEnabled(Long userId, NotificationType type) {
        UserNotifySetting setting = settingMapper.selectOne(new LambdaQueryWrapper<UserNotifySetting>()
                .eq(UserNotifySetting::getUserId, userId));
        if (setting == null) {
            return true;
        }
        return switch (type) {
            case ANNOUNCEMENT -> Integer.valueOf(1).equals(setting.getAnnouncementOn());
            case APPOINTMENT -> Integer.valueOf(1).equals(setting.getAppointmentOn());
            case INTERACTION -> Integer.valueOf(1).equals(setting.getInteractionOn());
            case ASSESSMENT -> Integer.valueOf(1).equals(setting.getAssessmentOn());
        };
    }

    private Long parseUserId(String key) {
        try {
            return Long.parseLong(key.substring(RedisConstants.NOTIFY_UNREAD_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private NotificationResponseDTO toDTO(Notification n) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setId(n.getId());
        dto.setType(n.getType());
        dto.setTitle(n.getTitle());
        dto.setContent(n.getContent());
        dto.setRelatedType(n.getRelatedType());
        dto.setRelatedId(n.getRelatedId());
        dto.setIsRead(Integer.valueOf(1).equals(n.getIsRead()));
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }

    private Long currentUserId() {
        return com.cc.util.CurrentUserUtils.getCurrentUserId();
    }
}
