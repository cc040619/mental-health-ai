package com.cc.service;

import com.cc.common.RedisConstants;
import com.cc.dto.response.AppointmentStatsResponseDTO;
import com.cc.dto.response.AssessmentStatsResponseDTO;
import com.cc.dto.response.ConsultationStatsResponseDTO;
import com.cc.dto.response.ContentStatsResponseDTO;
import com.cc.dto.response.DashboardOverviewResponseDTO;
import com.cc.dto.response.DataAnalyticsResponseDTO;
import com.cc.dto.response.EmotionStatsResponseDTO;
import com.cc.dto.response.NameCountDTO;
import com.cc.dto.response.UserTrendResponseDTO;
import com.cc.enums.AssessmentLevel;
import com.cc.enums.SessionScene;
import com.cc.enums.StatsRange;
import com.cc.exception.BusinessException;
import com.cc.mapper.DataAnalyticsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 数据分析业务层，聚合各统计 SQL 并组装为前端 dashboard 所需结构。
 *
 * <p>统计口径（全站统一）：</p>
 * <ul>
 *   <li>活跃用户：范围内在 日记/会话/测评/预约 任一表有操作的去重用户数（今日=当日）；</li>
 *   <li>平均会话时长：会话最后一条消息时间 − started_at（分钟）；</li>
 *   <li>测评完成数：assessment_record.status='COMPLETED'，按 completed_at 归属周期；</li>
 *   <li>预约完成率：COMPLETED / (总数 − CANCELED)，分母为 0 返回 null；</li>
 *   <li>环比：上一同长周期；同比：去年同期；基线为 0/数据不足时返回 null，前端显示「—」。</li>
 * </ul>
 *
 * <p>缓存：聚合结果 JSON 存 Redis（overview 5min、测评含高危预警 2min、其余 10min），
 * 定时任务每 10 分钟预热 TODAY/7D/30D；CUSTOM 范围不缓存、实时计算（低频）。
 * 依赖表（assessment_record/course/community_post/appointment）缺失时捕获 DataAccessException
 * 降级为空数据，表创建后自动恢复，无需改代码。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@Slf4j
@Service
public class DataAnalyticsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final long MAX_CUSTOM_DAYS = 366L;
    private static final String MODULE_OVERVIEW = "overview";
    private static final String MODULE_USER_TREND = "user-trend";
    private static final String MODULE_CONSULTATION = "consultation-stats";
    private static final String MODULE_ASSESSMENT = "assessment-stats";
    private static final String MODULE_EMOTION = "emotion-stats";
    private static final String MODULE_CONTENT = "content-stats";
    private static final String MODULE_APPOINTMENT = "appointment-stats";

    @Resource
    private DataAnalyticsMapper dataAnalyticsMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 解析后的统计周期（半开区间，SQL 统一使用 [start, endExclusive)）
     */
    record DateRange(StatsRange range, LocalDate start, LocalDate end,
                     LocalDate prevStart, LocalDate prevEnd,
                     LocalDate yoyStart, LocalDate yoyEnd) {
    }

    // ==================== 公开统计接口 ====================

    /** 核心指标卡（带环比/同比），兼容旧版概览字段 */
    public DashboardOverviewResponseDTO overview(String range, String startDate, String endDate) {
        DateRange dr = resolveRange(range, startDate, endDate);
        return queryDashboard(MODULE_OVERVIEW, dr, null, RedisConstants.DASHBOARD_OVERVIEW_TTL_MINUTES,
                DashboardOverviewResponseDTO.class, () -> buildOverview(dr));
    }

    /** 用户增长趋势：新增用户 + 累计用户，granularity=day/month */
    public UserTrendResponseDTO userTrend(String range, String startDate, String endDate, String granularity) {
        DateRange dr = resolveRange(range, startDate, endDate);
        String gran = "month".equalsIgnoreCase(granularity) ? "month" : "day";
        return queryDashboard(MODULE_USER_TREND, dr, gran, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                UserTrendResponseDTO.class, () -> buildUserTrend(dr, gran));
    }

    /** 咨询数据统计 */
    public ConsultationStatsResponseDTO consultationStats(String range, String startDate, String endDate) {
        DateRange dr = resolveRange(range, startDate, endDate);
        return queryDashboard(MODULE_CONSULTATION, dr, null, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                ConsultationStatsResponseDTO.class, () -> buildConsultationStats(dr));
    }

    /** 测评数据分析（含高危用户预警） */
    public AssessmentStatsResponseDTO assessmentStats(String range, String startDate, String endDate) {
        DateRange dr = resolveRange(range, startDate, endDate);
        return queryDashboard(MODULE_ASSESSMENT, dr, null, RedisConstants.DASHBOARD_RISK_TTL_MINUTES,
                AssessmentStatsResponseDTO.class, () -> buildAssessmentStats(dr));
    }

    /** 情绪大数据（分布 + 近30天热力图） */
    public EmotionStatsResponseDTO emotionStats(String range, String startDate, String endDate) {
        DateRange dr = resolveRange(range, startDate, endDate);
        return queryDashboard(MODULE_EMOTION, dr, null, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                EmotionStatsResponseDTO.class, () -> buildEmotionStats(dr));
    }

    /** 内容排行 TOP10（与日期范围无关） */
    public ContentStatsResponseDTO contentStats() {
        String cacheKey = RedisConstants.buildDashboardKey(MODULE_CONTENT);
        ContentStatsResponseDTO cached = getFromCache(cacheKey, ContentStatsResponseDTO.class);
        if (cached != null) {
            return cached;
        }
        ContentStatsResponseDTO data = buildContentStats();
        putCache(cacheKey, data, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES);
        return data;
    }

    /** 预约统计 */
    public AppointmentStatsResponseDTO appointmentStats(String range, String startDate, String endDate) {
        DateRange dr = resolveRange(range, startDate, endDate);
        return queryDashboard(MODULE_APPOINTMENT, dr, null, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                AppointmentStatsResponseDTO.class, () -> buildAppointmentStats(dr));
    }

    /**
     * 缓存预热：每 10 分钟由定时任务调用，预计算 TODAY/7D/30D 全部模块；
     * 单模块失败不影响其余模块（异常仅记录日志）。
     */
    public void warmUpCache() {
        for (StatsRange preset : List.of(StatsRange.TODAY, StatsRange.D7, StatsRange.D30)) {
            DateRange dr = resolveRange(preset.getCode(), null, null);
            warmOne(MODULE_OVERVIEW, dr, null, RedisConstants.DASHBOARD_OVERVIEW_TTL_MINUTES,
                    DashboardOverviewResponseDTO.class, () -> buildOverview(dr));
            warmOne(MODULE_USER_TREND, dr, "day", RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                    UserTrendResponseDTO.class, () -> buildUserTrend(dr, "day"));
            warmOne(MODULE_CONSULTATION, dr, null, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                    ConsultationStatsResponseDTO.class, () -> buildConsultationStats(dr));
            warmOne(MODULE_ASSESSMENT, dr, null, RedisConstants.DASHBOARD_RISK_TTL_MINUTES,
                    AssessmentStatsResponseDTO.class, () -> buildAssessmentStats(dr));
            warmOne(MODULE_EMOTION, dr, null, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                    EmotionStatsResponseDTO.class, () -> buildEmotionStats(dr));
            warmOne(MODULE_APPOINTMENT, dr, null, RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES,
                    AppointmentStatsResponseDTO.class, () -> buildAppointmentStats(dr));
        }
        try {
            putCache(RedisConstants.buildDashboardKey(MODULE_CONTENT), buildContentStats(),
                    RedisConstants.DASHBOARD_DEFAULT_TTL_MINUTES);
        } catch (Exception e) {
            log.warn("看板预热失败 module={}: {}", MODULE_CONTENT, e.getMessage());
        }
    }

    // ==================== 周期解析与参数构建 ====================

    /**
     * 解析范围参数为统计周期对象（含上一同长周期、去年同期）。
     * 包级可见供单元测试直接验证。
     */
    DateRange resolveRange(String rangeCode, String startDate, String endDate) {
        StatsRange range = StatsRange.fromCode(rangeCode);
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        switch (range) {
            case TODAY -> {
                start = today;
                end = today;
            }
            case D7 -> {
                start = today.minusDays(6);
                end = today;
            }
            case D30 -> {
                start = today.minusDays(29);
                end = today;
            }
            case CUSTOM -> {
                if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                    throw new BusinessException("自定义范围需传入起止日期");
                }
                try {
                    start = LocalDate.parse(startDate, DATE_FORMATTER);
                    end = LocalDate.parse(endDate, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    throw new BusinessException("日期格式不正确，应为 yyyy-MM-dd");
                }
                if (start.isAfter(end)) {
                    throw new BusinessException("开始日期不能晚于结束日期");
                }
                if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_CUSTOM_DAYS) {
                    throw new BusinessException("查询时间跨度不能超过1年");
                }
            }
            default -> throw new BusinessException("时间范围参数不正确");
        }
        long length = ChronoUnit.DAYS.between(start, end) + 1;
        return new DateRange(range, start, end,
                start.minusDays(length), start.minusDays(1),
                start.minusYears(1), end.minusYears(1));
    }

    /**
     * 构建 SQL 参数 Map，日期均为半开区间 [start, end+1day)
     */
    private Map<String, Object> buildParams(DateRange dr) {
        LocalDate today = LocalDate.now();
        LocalDate yoyToday = today.minusYears(1);
        Map<String, Object> p = new HashMap<>();
        p.put("td", today);
        p.put("tm", today.plusDays(1));
        p.put("yd", today.minusDays(1));
        p.put("yt", yoyToday);
        p.put("ytm", yoyToday.plusDays(1));
        p.put("rs", dr.start());
        p.put("re", dr.end().plusDays(1));
        p.put("ps", dr.prevStart());
        p.put("pe", dr.prevEnd().plusDays(1));
        p.put("ys", dr.yoyStart());
        p.put("ye", dr.yoyEnd().plusDays(1));
        p.put("h30s", today.minusDays(29));
        return p;
    }

    // ==================== 各模块组装 ====================

    /**
     * 核心指标卡 + 旧版概览兼容字段
     */
    private DashboardOverviewResponseDTO buildOverview(DateRange dr) {
        Map<String, Object> p = buildParams(dr);
        Map<String, Object> core = dataAnalyticsMapper.selectCoreMetrics(p);

        // 活跃用户：四表联合，依赖表缺失时降级为日记+会话
        Map<String, Object> active = safeRow(() -> dataAnalyticsMapper.selectActiveUsers(p),
                "assessment_record/appointment");
        if (active == null) {
            active = dataAnalyticsMapper.selectActiveUsersBase(p);
        }
        // 测评完成数、预约数：依赖表缺失降级为 0
        Map<String, Object> assessment = safeRow(() -> dataAnalyticsMapper.selectAssessmentCounts(p),
                "assessment_record");
        Map<String, Object> appointment = safeRow(() -> dataAnalyticsMapper.selectAppointmentCounts(p),
                "appointment");

        long totalUsers = toLong(core.get("totalUsers"));
        long todayNew = toLong(core.get("todayNewUsers"));
        long activeUsers = toLong(active.get("activeUsers"));
        long sessions = toLong(core.get("sessions"));
        long assessments = toLong(assessment, "completed");
        long appointments = toLong(appointment, "total");

        DashboardOverviewResponseDTO.MetricItem totalUsersItem = DashboardOverviewResponseDTO.MetricItem.builder()
                .value(totalUsers)
                .mom(changeRate(toLong(core.get("newUsers")), toLong(core.get("prevNewUsers"))))
                .yoy(changeRate(totalUsers, toLong(core.get("usersBeforeYoyEnd"))))
                .build();
        DashboardOverviewResponseDTO.MetricItem todayNewItem = DashboardOverviewResponseDTO.MetricItem.builder()
                .value(todayNew)
                .mom(changeRate(todayNew, toLong(core.get("yesterdayNewUsers"))))
                .yoy(changeRate(todayNew, toLong(core.get("yoyTodayNewUsers"))))
                .build();
        DashboardOverviewResponseDTO.MetricItem activeItem = DashboardOverviewResponseDTO.MetricItem.builder()
                .value(activeUsers)
                .mom(changeRate(activeUsers, toLong(active.get("prevActiveUsers"))))
                .yoy(changeRate(activeUsers, toLong(active.get("yoyActiveUsers"))))
                .build();
        DashboardOverviewResponseDTO.MetricItem sessionsItem = DashboardOverviewResponseDTO.MetricItem.builder()
                .value(sessions)
                .mom(changeRate(sessions, toLong(core.get("prevSessions"))))
                .yoy(changeRate(sessions, toLong(core.get("yoySessions"))))
                .build();
        DashboardOverviewResponseDTO.MetricItem assessmentsItem = DashboardOverviewResponseDTO.MetricItem.builder()
                .value(assessments)
                .mom(changeRate(assessments, toLong(assessment, "prevCompleted")))
                .yoy(changeRate(assessments, toLong(assessment, "yoyCompleted")))
                .build();
        DashboardOverviewResponseDTO.MetricItem appointmentsItem = DashboardOverviewResponseDTO.MetricItem.builder()
                .value(appointments)
                .mom(changeRate(appointments, toLong(appointment, "prevTotal")))
                .yoy(changeRate(appointments, toLong(appointment, "yoyTotal")))
                .build();

        return DashboardOverviewResponseDTO.builder()
                .totalUsers(totalUsersItem)
                .todayNewUsers(todayNewItem)
                .activeUsers(activeItem)
                .sessions(sessionsItem)
                .assessments(assessmentsItem)
                .appointments(appointmentsItem)
                // 旧版字段（结构不变，按所选范围填充序列）
                .systemOverview(DataAnalyticsResponseDTO.SystemOverview.builder()
                        .totalUsers(totalUsers)
                        .activeUsers(activeUsers)
                        .totalDiaries(toLong(core.get("totalDiaries")))
                        .todayNewDiaries(toLong(core.get("todayNewDiaries")))
                        .totalSessions(toLong(core.get("totalSessionsAll")))
                        .todayNewSessions(dataAnalyticsMapper.countTodayNewSessions())
                        .avgMoodScore(toDouble(core.get("avgMoodScoreAll")))
                        .build())
                .emotionTrend(buildLegacyEmotionTrend(p, dr))
                .consultationStats(buildLegacyConsultation(p, dr, toLong(core.get("totalSessionsAll"))))
                .userActivity(buildLegacyActivity(p, dr))
                .build();
    }

    /**
     * 用户增长趋势：按日/按月分组，累计用户在 Service 层累加（单调递增）
     */
    private UserTrendResponseDTO buildUserTrend(DateRange dr, String granularity) {
        Map<String, Object> p = buildParams(dr);
        boolean month = "month".equals(granularity);
        p.put("fmt", month ? "%Y-%m" : "%Y-%m-%d");
        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : dataAnalyticsMapper.selectUserTrend(p)) {
            countMap.put(Objects.toString(row.get("label"), ""), toLong(row.get("cnt")));
        }

        List<String> dates = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        if (month) {
            YearMonth last = YearMonth.from(dr.end());
            for (YearMonth ym = YearMonth.from(dr.start()); !ym.isAfter(last); ym = ym.plusMonths(1)) {
                String label = ym.format(MONTH_FORMATTER);
                dates.add(label);
                newUsers.add(countMap.getOrDefault(label, 0L));
            }
        } else {
            for (LocalDate date : buildDateSeries(dr.start(), dr.end())) {
                String label = date.format(DATE_FORMATTER);
                dates.add(label);
                newUsers.add(countMap.getOrDefault(label, 0L));
            }
        }

        // 累计口径：范围起始前的用户总量，逐点累加范围内新增
        long cumulative = dataAnalyticsMapper.countUsersBefore(dr.start());
        List<Long> cumulativeUsers = new ArrayList<>();
        for (Long count : newUsers) {
            cumulative += count;
            cumulativeUsers.add(cumulative);
        }
        return UserTrendResponseDTO.builder()
                .granularity(granularity)
                .dates(dates)
                .newUsers(newUsers)
                .cumulativeUsers(cumulativeUsers)
                .build();
    }

    /**
     * 咨询统计：每日趋势 + 平均时长 + 主题分布（场景码转中文）+ 消息情绪分布
     */
    private ConsultationStatsResponseDTO buildConsultationStats(DateRange dr) {
        Map<String, Object> p = buildParams(dr);
        List<Map<String, Object>> rows = dataAnalyticsMapper.selectSessionTrendWithAvg(p);
        Map<String, Map<String, Object>> trendMap = toKeyedMap(rows);
        double avgDuration = rows.isEmpty() ? 0.0 : toDouble(rows.get(0).get("avgDurationMinutes"));

        List<ConsultationStatsResponseDTO.DailyTrend> dailyTrend = new ArrayList<>();
        for (LocalDate date : buildDateSeries(dr.start(), dr.end())) {
            String key = date.format(DATE_FORMATTER);
            Map<String, Object> row = trendMap.get(key);
            dailyTrend.add(ConsultationStatsResponseDTO.DailyTrend.builder()
                    .date(key)
                    .sessionCount(row != null ? toLong(row.get("sessionCount")) : 0L)
                    .userCount(row != null ? toLong(row.get("userCount")) : 0L)
                    .build());
        }

        List<NameCountDTO> scenes = new ArrayList<>();
        for (Map<String, Object> row : safeList(() -> dataAnalyticsMapper.selectSceneDistribution(p),
                "consultation_session.scene")) {
            NameCountDTO item = toNameCount(row);
            SessionScene scene = SessionScene.fromCode(item.getName());
            if (scene != null) {
                item.setName(scene.getLabel());
            }
            scenes.add(item);
        }
        List<NameCountDTO> emotions = safeList(
                () -> dataAnalyticsMapper.selectMessageEmotionDistribution(p), "consultation_message")
                .stream().map(this::toNameCount).toList();

        return ConsultationStatsResponseDTO.builder()
                .dailyTrend(dailyTrend)
                .avgDurationMinutes(avgDuration)
                .sceneDistribution(scenes)
                .emotionDistribution(emotions)
                .build();
    }

    /**
     * 测评分析：量表次数 + 固定四档等级分布 + 高危用户 TOP10；依赖表整体缺失时返回空结构
     */
    private AssessmentStatsResponseDTO buildAssessmentStats(DateRange dr) {
        Map<String, Object> p = buildParams(dr);
        try {
            List<NameCountDTO> scaleCounts = dataAnalyticsMapper.selectScaleCounts(p).stream()
                    .map(this::toNameCount).toList();

            Map<String, Long> levelMap = new HashMap<>();
            for (Map<String, Object> row : dataAnalyticsMapper.selectLevelCounts(p)) {
                levelMap.put(Objects.toString(row.get("name"), ""), toLong(row.get("value")));
            }
            List<AssessmentStatsResponseDTO.LevelCount> levelDistribution = Arrays.stream(AssessmentLevel.values())
                    .map(level -> AssessmentStatsResponseDTO.LevelCount.builder()
                            .level(level.getCode())
                            .label(level.getLabel())
                            .value(levelMap.getOrDefault(level.getCode(), 0L))
                            .build())
                    .toList();

            List<AssessmentStatsResponseDTO.RiskUserItem> riskUsers = dataAnalyticsMapper.selectRiskUsers(p)
                    .stream().map(this::toRiskUser).toList();

            return AssessmentStatsResponseDTO.builder()
                    .scaleCounts(scaleCounts)
                    .levelDistribution(levelDistribution)
                    .riskUsers(riskUsers)
                    .build();
        } catch (DataAccessException e) {
            log.warn("统计模块依赖表未创建，跳过: assessment_record/assessment_scale");
            return AssessmentStatsResponseDTO.builder()
                    .scaleCounts(List.of())
                    .levelDistribution(List.of())
                    .riskUsers(List.of())
                    .build();
        }
    }

    /**
     * 情绪大数据：范围内主导情绪分布 + 近30天日期×情绪热力图
     */
    private EmotionStatsResponseDTO buildEmotionStats(DateRange dr) {
        Map<String, Object> p = buildParams(dr);
        List<NameCountDTO> distribution = dataAnalyticsMapper.selectDiaryEmotionDistribution(p).stream()
                .map(this::toNameCount).toList();

        List<String> dates = buildDateSeries(LocalDate.now().minusDays(29), LocalDate.now()).stream()
                .map(date -> date.format(DATE_FORMATTER)).toList();
        Map<String, Integer> dateIndex = new HashMap<>();
        for (int i = 0; i < dates.size(); i++) {
            dateIndex.put(dates.get(i), i);
        }

        List<Map<String, Object>> rows = dataAnalyticsMapper.selectEmotionHeatmap(p);
        Map<String, Long> totals = new HashMap<>();
        for (Map<String, Object> row : rows) {
            totals.merge(Objects.toString(row.get("emotion"), ""), toLong(row.get("cnt")), Long::sum);
        }
        // Y 轴情绪按近30天总量倒序，同名次按字典序，保证渲染稳定
        List<String> emotions = totals.keySet().stream()
                .sorted(Comparator.comparingLong((String emotion) -> totals.getOrDefault(emotion, 0L))
                        .reversed().thenComparing(emotion -> emotion))
                .toList();
        Map<String, Integer> emotionIndex = new HashMap<>();
        for (int i = 0; i < emotions.size(); i++) {
            emotionIndex.put(emotions.get(i), i);
        }
        List<int[]> heatData = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer x = dateIndex.get(Objects.toString(row.get("date"), ""));
            Integer y = emotionIndex.get(Objects.toString(row.get("emotion"), ""));
            if (x != null && y != null) {
                heatData.add(new int[]{x, y, Math.toIntExact(toLong(row.get("cnt")))});
            }
        }

        return EmotionStatsResponseDTO.builder()
                .emotionDistribution(distribution)
                .heatmap(EmotionStatsResponseDTO.Heatmap.builder()
                        .dates(dates)
                        .emotions(emotions)
                        .data(heatData)
                        .build())
                .build();
    }

    /**
     * 内容排行：文章/课程/帖子各 TOP10，课程与帖子表缺失时各自降级为空
     */
    private ContentStatsResponseDTO buildContentStats() {
        List<NameCountDTO> articles = dataAnalyticsMapper.selectTopArticles().stream()
                .map(this::toNameCount).toList();
        List<NameCountDTO> courses = safeList(dataAnalyticsMapper::selectTopCourses, "course").stream()
                .map(this::toNameCount).toList();
        List<NameCountDTO> posts = safeList(dataAnalyticsMapper::selectTopPosts, "community_post").stream()
                .map(this::toNameCount).toList();
        return ContentStatsResponseDTO.builder()
                .articles(articles)
                .courses(courses)
                .posts(posts)
                .build();
    }

    /**
     * 预约统计：每日趋势 + 咨询师排行 + 完成率；appointment 表缺失时返回空结构
     */
    private AppointmentStatsResponseDTO buildAppointmentStats(DateRange dr) {
        Map<String, Object> p = buildParams(dr);
        try {
            Map<String, Long> trendMap = new LinkedHashMap<>();
            for (Map<String, Object> row : dataAnalyticsMapper.selectAppointmentDailyTrend(p)) {
                trendMap.put(Objects.toString(row.get("date"), ""), toLong(row.get("value")));
            }
            List<NameCountDTO> dailyTrend = new ArrayList<>();
            for (LocalDate date : buildDateSeries(dr.start(), dr.end())) {
                String key = date.format(DATE_FORMATTER);
                dailyTrend.add(NameCountDTO.builder().name(key).value(trendMap.getOrDefault(key, 0L)).build());
            }

            Map<String, Object> rateRow = dataAnalyticsMapper.selectAppointmentRate(p);
            long effective = toLong(rateRow.get("effective"));
            long completed = toLong(rateRow.get("completed"));
            // ponytail: 分母为 0（无有效预约）时完成率无意义，返回 null 由前端显示「—」
            Double completionRate = effective == 0L ? null
                    : Math.round(completed * 1000.0 / effective) / 10.0;

            List<NameCountDTO> ranking = dataAnalyticsMapper.selectConsultantRanking(p).stream()
                    .map(this::toNameCount).toList();

            List<NameCountDTO> statusDist = dataAnalyticsMapper.selectAppointmentStatusDist(p).stream()
                    .map(this::toNameCount).toList();
            List<NameCountDTO> hotHours = dataAnalyticsMapper.selectAppointmentHotHours(p).stream()
                    .map(this::toNameCount).toList();

            return AppointmentStatsResponseDTO.builder()
                    .dailyTrend(dailyTrend)
                    .consultantRanking(ranking)
                    .completionRate(completionRate)
                    .statusDistribution(statusDist)
                    .hotHours(hotHours)
                    .build();
        } catch (DataAccessException e) {
            log.warn("统计模块依赖表未创建，跳过: appointment/consultant");
            return AppointmentStatsResponseDTO.builder()
                    .dailyTrend(List.of())
                    .consultantRanking(List.of())
                    .completionRate(null)
                    .statusDistribution(List.of())
                    .hotHours(List.of())
                    .build();
        }
    }

    // ==================== 旧版概览序列（保持响应字段兼容） ====================

    private List<DataAnalyticsResponseDTO.EmotionTrendItem> buildLegacyEmotionTrend(Map<String, Object> p,
                                                                                    DateRange dr) {
        Map<String, Map<String, Object>> trendMap = toKeyedMap(dataAnalyticsMapper.selectEmotionTrend(p));
        List<DataAnalyticsResponseDTO.EmotionTrendItem> items = new ArrayList<>();
        for (LocalDate date : buildDateSeries(dr.start(), dr.end())) {
            String key = date.format(DATE_FORMATTER);
            Map<String, Object> row = trendMap.get(key);
            items.add(DataAnalyticsResponseDTO.EmotionTrendItem.builder()
                    .date(key)
                    .avgMoodScore(row != null ? toDouble(row.get("avgMoodScore")) : 0.0)
                    .recordCount(row != null ? toLong(row.get("recordCount")) : 0L)
                    .build());
        }
        return items;
    }

    private DataAnalyticsResponseDTO.ConsultationStats buildLegacyConsultation(Map<String, Object> p,
                                                                               DateRange dr,
                                                                               long totalSessionsAll) {
        List<Map<String, Object>> rows = dataAnalyticsMapper.selectSessionTrendWithAvg(p);
        Map<String, Map<String, Object>> trendMap = toKeyedMap(rows);
        double avgDuration = rows.isEmpty() ? 0.0 : toDouble(rows.get(0).get("avgDurationMinutes"));

        List<DataAnalyticsResponseDTO.DailyTrendItem> dailyTrend = new ArrayList<>();
        for (LocalDate date : buildDateSeries(dr.start(), dr.end())) {
            String key = date.format(DATE_FORMATTER);
            Map<String, Object> row = trendMap.get(key);
            long sessionCount = row != null ? toLong(row.get("sessionCount")) : 0L;
            long userCount = row != null ? toLong(row.get("userCount")) : 0L;
            dailyTrend.add(DataAnalyticsResponseDTO.DailyTrendItem.builder()
                    .date(key)
                    .sessionCount(sessionCount)
                    .userCount(userCount)
                    .build());
        }
        // 旧版 SystemOverview.todayNewSessions 以数据库当日实时值为准（在 buildOverview 单独设置）
        return DataAnalyticsResponseDTO.ConsultationStats.builder()
                .totalSessions(totalSessionsAll)
                .avgDurationMinutes(avgDuration)
                .dailyTrend(dailyTrend)
                .build();
    }

    private List<DataAnalyticsResponseDTO.UserActivityItem> buildLegacyActivity(Map<String, Object> p,
                                                                                DateRange dr) {
        Map<String, Map<String, Object>> newMap = new HashMap<>();
        Map<String, Map<String, Object>> diaryMap = new HashMap<>();
        Map<String, Map<String, Object>> consultMap = new HashMap<>();
        Map<String, Map<String, Object>> activeMap = new HashMap<>();
        for (Map<String, Object> row : dataAnalyticsMapper.selectActivityUnion(p)) {
            switch (Objects.toString(row.get("type"), "")) {
                case "new" -> newMap.put(Objects.toString(row.get("date"), ""), row);
                case "diary" -> diaryMap.put(Objects.toString(row.get("date"), ""), row);
                case "consult" -> consultMap.put(Objects.toString(row.get("date"), ""), row);
                default -> activeMap.put(Objects.toString(row.get("date"), ""), row);
            }
        }
        List<DataAnalyticsResponseDTO.UserActivityItem> items = new ArrayList<>();
        for (LocalDate date : buildDateSeries(dr.start(), dr.end())) {
            String key = date.format(DATE_FORMATTER);
            items.add(DataAnalyticsResponseDTO.UserActivityItem.builder()
                    .date(key)
                    .activeUsers(activeMap.containsKey(key) ? toLong(activeMap.get(key).get("cnt")) : 0L)
                    .newUsers(newMap.containsKey(key) ? toLong(newMap.get(key).get("cnt")) : 0L)
                    .diaryUsers(diaryMap.containsKey(key) ? toLong(diaryMap.get(key).get("cnt")) : 0L)
                    .consultationUsers(consultMap.containsKey(key) ? toLong(consultMap.get(key).get("cnt")) : 0L)
                    .build());
        }
        return items;
    }

    // ==================== 缓存 ====================

    /**
     * 带缓存的查询模板：CUSTOM 实时计算；其余先查 Redis，未命中构建后回写
     */
    private <T> T queryDashboard(String module, DateRange dr, String suffix, long ttlMinutes,
                                 Class<T> type, Supplier<T> builder) {
        if (dr.range() != StatsRange.CUSTOM) {
            T cached = getFromCache(cacheKey(module, dr, suffix), type);
            if (cached != null) {
                return cached;
            }
        }
        T data = builder.get();
        if (dr.range() != StatsRange.CUSTOM) {
            putCache(cacheKey(module, dr, suffix), data, ttlMinutes);
        }
        return data;
    }

    private <T> void warmOne(String module, DateRange dr, String suffix, long ttlMinutes,
                             Class<T> type, Supplier<T> builder) {
        try {
            putCache(cacheKey(module, dr, suffix), builder.get(), ttlMinutes);
        } catch (Exception e) {
            log.warn("看板预热失败 module={} range={}: {}", module, dr.range().getCode(), e.getMessage());
        }
    }

    private String cacheKey(String module, DateRange dr, String suffix) {
        return RedisConstants.buildDashboardKey(module, dr.range().getCode(),
                dr.start().format(DATE_FORMATTER), dr.end().format(DATE_FORMATTER), suffix);
    }

    private <T> T getFromCache(String key, Class<T> type) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            // 缓存内容异常不影响实时查询，按未命中处理
            log.warn("看板缓存读取失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void putCache(String key, Object value, long ttlMinutes) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value),
                    ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("看板缓存写入失败 key={}: {}", key, e.getMessage());
        }
    }

    // ==================== 降级 / 转换工具 ====================

    /** 查询单行聚合，依赖表不存在时返回 null（调用方自行回退） */
    private Map<String, Object> safeRow(Supplier<Map<String, Object>> supplier, String table) {
        try {
            return supplier.get();
        } catch (DataAccessException e) {
            log.warn("统计模块依赖表未创建，跳过: {}", table);
            return null;
        }
    }

    /** 查询列表聚合，依赖表不存在时返回空列表 */
    private List<Map<String, Object>> safeList(Supplier<List<Map<String, Object>>> supplier, String table) {
        try {
            return supplier.get();
        } catch (DataAccessException e) {
            log.warn("统计模块依赖表未创建，跳过: {}", table);
            return List.of();
        }
    }

    private NameCountDTO toNameCount(Map<String, Object> row) {
        return NameCountDTO.builder()
                .name(Objects.toString(row.get("name"), ""))
                .value(toLong(row.get("value")))
                .build();
    }

    /** 高危用户行转换：手机号脱敏、风险类型/最高等级推导，仅保留元数据 */
    private AssessmentStatsResponseDTO.RiskUserItem toRiskUser(Map<String, Object> row) {
        Set<String> levels = Set.copyOf(Arrays.stream(Objects.toString(row.get("levels"), "").split(","))
                .filter(s -> !s.isBlank())
                .toList());
        boolean severe = levels.contains(AssessmentLevel.SEVERE.getCode());
        String highest = levels.stream()
                .map(AssessmentLevel::fromCode)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(Enum::ordinal))
                .map(AssessmentLevel::getCode)
                .orElse("");
        Object lastTime = row.get("lastTime");
        return AssessmentStatsResponseDTO.RiskUserItem.builder()
                .nickname(Objects.toString(row.get("nickname"), "用户"))
                .phone(maskPhone(Objects.toString(row.get("phone"), null)))
                .riskType(severe ? "重度测评预警" : "高危风险标记")
                .riskLevel(highest)
                .lastTime(lastTime == null ? "" : formatDateTime(lastTime))
                .count(toLong(row.get("cnt")))
                .build();
    }

    /** 手机号脱敏 138****1234；非 11 位号码原样返回（不臆造掩码） */
    private String maskPhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.matches("\\d{11}") ? phone.substring(0, 3) + "****" + phone.substring(7) : phone;
    }

    private String formatDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(DATE_TIME_FORMATTER);
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().format(DATE_TIME_FORMATTER);
        }
        return value.toString();
    }

    /** 环比/同比百分比，保留 1 位小数；基线为 0（数据不足）返回 null */
    private Double changeRate(long current, long previous) {
        if (previous == 0L) {
            return null;
        }
        return Math.round((current - previous) * 1000.0 / previous) / 10.0;
    }

    /**
     * 将 SQL 聚合结果按 date 字段转为 Map，便于按日期补齐
     */
    private Map<String, Map<String, Object>> toKeyedMap(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object date = row.get("date");
            if (date != null) {
                map.put(date.toString(), row);
            }
        }
        return map;
    }

    /**
     * 连续日期序列（含首尾，升序）；由旧 lastSevenDays 泛化而来
     */
    private List<LocalDate> buildDateSeries(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    /**
     * 数值安全转换（MySQL 聚合结果可能为 BigDecimal/Long/BigInteger）
     */
    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private long toLong(Map<String, Object> row, String key) {
        return row == null ? 0L : toLong(row.get(key));
    }

    private Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
