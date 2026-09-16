package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.common.RedisConstants;
import com.cc.dto.response.AdminUserDetailResponseDTO;
import com.cc.dto.response.AdminUserListResponseDTO;
import com.cc.dto.response.AdminUserStatsResponseDTO;
import com.cc.dto.response.NameCountDTO;
import com.cc.entity.EmotionDiary;
import com.cc.entity.AssessmentRecord;
import com.cc.entity.ConsultationSession;
import com.cc.entity.User;
import com.cc.enums.UserStatus;
import com.cc.exception.BusinessException;
import com.cc.mapper.AssessmentRecordMapper;
import com.cc.mapper.ConsultationSessionMapper;
import com.cc.mapper.EmotionDiaryMapper;
import com.cc.mapper.UserMapper;
import com.cc.service.convert.UserConvert;
import com.cc.util.CurrentUserUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 管理端用户业务层：与用户端 UserService 分离，避免互相污染。
 *
 * <p>职责：用户列表/详情/状态操作/统计 + 详情 Tab 5 类记录查询编排（按 userId 分页委托各业务 Service）。
 * 强制下线/禁用/重置密码统一调用 {@link TokenService#removeLoginToken(Long)} 让 Redis 白名单 token 立即失效。
 * 用户统计走 Redis 缓存（TTL 10 分钟），冷启动回源 DB 一次聚合三组数据。</p>
 *
 * <p>依赖表缺失降级：详情 Tab 各记录查询与统计中依赖 3.2/3.5/3.6/3.7 表的 SQL 用 try-catch 包
 * DataAccessException，捕获后 log.warn + 返回空数据，表建好后自动恢复（沿用 4.1 模式）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class AdminUserService {

    /** 默认重置密码（BCrypt 加密存储；不入日志） */
    private static final String DEFAULT_RESET_PASSWORD = "123456";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private UserMapper userMapper;
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;
    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;
    @Resource
    private AssessmentRecordMapper assessmentRecordMapper;
    @Resource
    private ConsultationSessionService consultationSessionService;
    @Resource
    private AssessmentService assessmentService;
    @Resource
    private EmotionDiaryService emotionDiaryService;
    @Resource
    private CommunityService communityService;
    @Resource
    private AppointmentService appointmentService;
    @Resource
    private TokenService tokenService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 列表 ====================

    /**
     * 用户列表：keyword 匹配昵称或用户名（中缀 LIKE），phone 中缀 LIKE，status eq，created_at between。
     * ponytail: 数据量小场景 LIKE 中缀可接受；数据量大时改为前缀匹配 + idx_username 索引
     */
    public Map<String, Object> listUsers(String keyword, String phone, Integer status,
                                         String startDate, String endDate, long page, long size) {
        long cur = Math.max(page, 1L);
        long sz = Math.min(Math.max(size, 1L), 20L);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getNickname, kw).or().like(User::getUsername, kw));
        }
        if (StringUtils.hasText(phone)) {
            wrapper.like(User::getPhone, phone.trim());
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        // ponytail: 注册时间区间按半开区间 [start, end+1day) 处理避免边界遗漏
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start != null) {
            wrapper.ge(User::getCreatedAt, start.atStartOfDay());
        }
        if (end != null) {
            wrapper.lt(User::getCreatedAt, end.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> userPage = userMapper.selectPage(new Page<>(cur, sz), wrapper);
        List<AdminUserListResponseDTO> records = userPage.getRecords().stream()
                .map(UserConvert::entityToAdminListResponse)
                .toList();
        return Map.of("records", records, "total", userPage.getTotal());
    }

    // ==================== 详情 ====================

    /**
     * 用户详情：基本信息 + 简要统计（日记数/会话数/测评数），统计依赖表缺失降级为 0
     */
    public AdminUserDetailResponseDTO getUserDetail(Long userId) {
        User user = mustGetUser(userId);
        long diaryCount = safeCount(() -> emotionDiaryMapper.selectCount(
                new LambdaQueryWrapper<EmotionDiary>().eq(EmotionDiary::getUserId, userId)), "emotion_diary");
        long sessionCount = safeCount(() -> consultationSessionMapper.selectCount(
                new LambdaQueryWrapper<ConsultationSession>().eq(ConsultationSession::getUserId, userId)),
                "consultation_session");
        long assessmentCount = safeCount(() -> assessmentRecordMapper.selectCount(
                new LambdaQueryWrapper<AssessmentRecord>().eq(AssessmentRecord::getUserId, userId)),
                "assessment_record");
        return UserConvert.entityToAdminDetailResponse(user, diaryCount, sessionCount, assessmentCount);
    }

    // ==================== 详情 Tab 5 类记录查询（懒加载） ====================

    /** 咨询记录 */
    public Map<String, Object> listUserConsultations(Long userId, long page, long size) {
        mustGetUser(userId);
        try {
            Page<com.cc.dto.response.ConsultationSessionResponseDTO> result =
                    consultationSessionService.pageByUserIdForAdmin(userId, page, size);
            return Map.of("records", result.getRecords(), "total", result.getTotal());
        } catch (DataAccessException e) {
            log.warn("用户管理-咨询记录查询依赖表未创建: consultation_session");
            return Map.of("records", List.of(), "total", 0L);
        }
    }

    /** 测评记录 */
    public Map<String, Object> listUserAssessments(Long userId, long page, long size) {
        mustGetUser(userId);
        try {
            return assessmentService.listRecordsForAdmin(userId, page, size);
        } catch (DataAccessException e) {
            log.warn("用户管理-测评记录查询依赖表未创建: assessment_record");
            return Map.of("records", List.of(), "total", 0L);
        }
    }

    /** 情绪日记 */
    public Map<String, Object> listUserDiaries(Long userId, long page, long size) {
        mustGetUser(userId);
        try {
            var result = emotionDiaryService.pageForAdminByUserId(userId, page, size);
            return Map.of("records", result.getRecords(), "total", result.getTotal());
        } catch (DataAccessException e) {
            log.warn("用户管理-情绪日记查询依赖表未创建: emotion_diary");
            return Map.of("records", List.of(), "total", 0L);
        }
    }

    /** 发帖记录 */
    public Map<String, Object> listUserPosts(Long userId, long page, long size) {
        mustGetUser(userId);
        try {
            return communityService.pagePostsByUserIdForAdmin(userId, page, size);
        } catch (DataAccessException e) {
            log.warn("用户管理-发帖记录查询依赖表未创建: community_post");
            return Map.of("records", List.of(), "total", 0L);
        }
    }

    /** 预约记录 */
    public Map<String, Object> listUserAppointments(Long userId, long page, long size) {
        mustGetUser(userId);
        try {
            return appointmentService.pageForAdminByUserId(userId, page, size);
        } catch (DataAccessException e) {
            log.warn("用户管理-预约记录查询依赖表未创建: appointment");
            return Map.of("records", List.of(), "total", 0L);
        }
    }

    // ==================== 账号操作 ====================

    /**
     * 禁用/启用：禁用时 status=0 + removeLoginToken 立即下线；
     * 禁止对当前登录管理员自己操作（防自禁/自重置）
     */
    public void updateStatus(Long userId, Integer status) {
        User user = mustGetUser(userId);
        ensureNotSelf(userId);
        UserStatus target = UserStatus.fromCode(status);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, target.getCode()));
        // 禁用时立即清 Redis 白名单 token，原会话即时失效
        if (target == UserStatus.DISABLED) {
            tokenService.removeLoginToken(userId);
        }
        log.info("管理员操作, userId={}, action=updateStatus, status={}", userId, target.getCode());
    }

    /**
     * 重置密码：重置为系统默认密码（BCrypt 加密存储）+ removeLoginToken 强制下线
     */
    public void resetPassword(Long userId) {
        mustGetUser(userId);
        ensureNotSelf(userId);

        String encodedPassword = passwordEncoder.encode(DEFAULT_RESET_PASSWORD);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, encodedPassword));
        tokenService.removeLoginToken(userId);
        log.info("管理员操作, userId={}, action=resetPassword", userId);
    }

    /**
     * 强制下线：removeLoginToken 删除 Redis 白名单 token，原 token 立即失效
     */
    public void forceLogout(Long userId) {
        mustGetUser(userId);
        tokenService.removeLoginToken(userId);
        log.info("管理员操作, userId={}, action=forceLogout", userId);
    }

    // ==================== 统计 ====================

    /**
     * 用户统计：性别分布 + 年龄段分布 + 注册趋势（近 30 天按日，缺失补零），走 Redis 缓存 TTL 10 分钟
     */
    public AdminUserStatsResponseDTO getUserStats() {
        try {
            String json = stringRedisTemplate.opsForValue().get(RedisConstants.USER_STATS_KEY);
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, AdminUserStatsResponseDTO.class);
            }
        } catch (Exception e) {
            log.warn("用户统计缓存读取失败，回源 DB: {}", e.getMessage());
        }
        AdminUserStatsResponseDTO data = buildUserStats();
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.USER_STATS_KEY,
                    objectMapper.writeValueAsString(data),
                    RedisConstants.USER_STATS_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("用户统计缓存写入失败（fail-open）: {}", e.getMessage());
        }
        return data;
    }

    private AdminUserStatsResponseDTO buildUserStats() {
        // 性别分布：固定三档 男/女/未知，0 值也返回保证饼图渲染稳定
        Map<Integer, Long> genderCounts = new HashMap<>();
        for (Map<String, Object> row : userMapper.selectGenderDistribution()) {
            genderCounts.put(((Number) row.get("gender")).intValue(), ((Number) row.get("cnt")).longValue());
        }
        List<NameCountDTO> genderDist = new ArrayList<>();
        genderDist.add(NameCountDTO.builder().name("男").value(genderCounts.getOrDefault(1, 0L)).build());
        genderDist.add(NameCountDTO.builder().name("女").value(genderCounts.getOrDefault(2, 0L)).build());
        genderDist.add(NameCountDTO.builder().name("未知").value(genderCounts.getOrDefault(0, 0L)).build());

        // 年龄段分布：固定六档
        Map<String, Long> ageCounts = new HashMap<>();
        for (Map<String, Object> row : userMapper.selectAgeDistribution()) {
            ageCounts.put(Objects.toString(row.get("label"), ""), ((Number) row.get("cnt")).longValue());
        }
        List<String> ageBuckets = List.of("0-18", "19-25", "26-35", "36-50", "50+", "未知");
        List<NameCountDTO> ageDist = new ArrayList<>();
        for (String label : ageBuckets) {
            ageDist.add(NameCountDTO.builder().name(label).value(ageCounts.getOrDefault(label, 0L)).build());
        }

        // 注册趋势：近 30 天按日补零
        Map<String, Long> trendMap = new HashMap<>();
        for (Map<String, Object> row : userMapper.selectRegistrationTrend()) {
            trendMap.put(Objects.toString(row.get("date"), ""), ((Number) row.get("cnt")).longValue());
        }
        LocalDate today = LocalDate.now();
        List<NameCountDTO> trend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            String date = today.minusDays(i).format(DATE_FORMATTER);
            trend.add(NameCountDTO.builder().name(date).value(trendMap.getOrDefault(date, 0L)).build());
        }

        return AdminUserStatsResponseDTO.builder()
                .genderDistribution(genderDist)
                .ageDistribution(ageDist)
                .registrationTrend(trend)
                .build();
    }

    // ==================== 内部工具 ====================

    private User mustGetUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 防管理员自禁/自重置：当前登录管理员不能对自己执行禁用/重置密码
     */
    private void ensureNotSelf(Long targetUserId) {
        Long currentUserId = CurrentUserUtils.getCurrentUserId();
        if (targetUserId.equals(currentUserId)) {
            throw new BusinessException("不能对当前登录账号执行此操作");
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            throw new BusinessException("日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    /**
     * 安全计数：依赖表缺失（DataAccessException）降级为 0，表建好后自动恢复
     */
    private long safeCount(CounterSupplier supplier, String table) {
        try {
            return supplier.count();
        } catch (DataAccessException e) {
            log.warn("用户管理-简要统计依赖表未创建，跳过: {}", table);
            return 0L;
        }
    }

    @FunctionalInterface
    private interface CounterSupplier {
        long count();
    }
}
