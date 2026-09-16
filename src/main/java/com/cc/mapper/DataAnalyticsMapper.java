package com.cc.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据分析聚合查询 Mapper。
 *
 * <p>日期范围统一通过 Map 参数绑定（键：rs/re 本周期、ps/pe 上一周期、ys/ye 去年同期、
 * td/tm 今日、yd 昨日、yt/ytm 去年今日，均为半开区间 [start, end)），禁止拼接 SQL；
 * 涉及 3.2/3.5/3.6/3.7 模块表的查询由 Service 层捕获 DataAccessException 降级，表创建后自动恢复。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@Mapper
public interface DataAnalyticsMapper {

    // ==================== 全量基础指标（表始终存在） ====================

    /** 用户总数 */
    @Select("SELECT COUNT(*) FROM user")
    long countUsers();

    /** 日记总数 */
    @Select("SELECT COUNT(*) FROM emotion_diary")
    long countDiaries();

    /** 今日新增日记数 */
    @Select("SELECT COUNT(*) FROM emotion_diary WHERE diary_date = #{td}")
    long countTodayNewDiaries(@Param("td") LocalDate today);

    /** 会话总数 */
    @Select("SELECT COUNT(*) FROM consultation_session")
    long countSessions();

    /** 今日新增会话数 */
    @Select("SELECT COUNT(*) FROM consultation_session WHERE DATE(started_at) = CURDATE()")
    long countTodayNewSessions();

    /** 全部日记的平均情绪评分 */
    @Select("SELECT IFNULL(ROUND(AVG(mood_score), 1), 0) FROM emotion_diary")
    double avgMoodScore();

    /** 范围起始日之前的累计用户数（累计曲线起点） */
    @Select("SELECT COUNT(*) FROM user WHERE created_at < #{date}")
    long countUsersBefore(@Param("date") LocalDate date);

    // ==================== 核心指标（一次组合查询，仅含始终存在的表） ====================

    /**
     * 核心指标单行：用户（总数/今日/昨日/去年今日/本周期/上周期/去年同期新增/一年前总量）、
     * 会话（本周期/上周期/去年同期）、旧版概览字段（日记总数/今日日记/会话总数/平均情绪）
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM user) AS totalUsers,
              (SELECT COUNT(*) FROM user WHERE created_at >= #{td} AND created_at < #{tm}) AS todayNewUsers,
              (SELECT COUNT(*) FROM user WHERE created_at >= #{yd} AND created_at < #{td}) AS yesterdayNewUsers,
              (SELECT COUNT(*) FROM user WHERE created_at >= #{yt} AND created_at < #{ytm}) AS yoyTodayNewUsers,
              (SELECT COUNT(*) FROM user WHERE created_at >= #{rs} AND created_at < #{re}) AS newUsers,
              (SELECT COUNT(*) FROM user WHERE created_at >= #{ps} AND created_at < #{pe}) AS prevNewUsers,
              (SELECT COUNT(*) FROM user WHERE created_at >= #{ys} AND created_at < #{ye}) AS yoyNewUsers,
              (SELECT COUNT(*) FROM user WHERE created_at < #{ye}) AS usersBeforeYoyEnd,
              (SELECT COUNT(*) FROM consultation_session WHERE started_at >= #{rs} AND started_at < #{re}) AS sessions,
              (SELECT COUNT(*) FROM consultation_session WHERE started_at >= #{ps} AND started_at < #{pe}) AS prevSessions,
              (SELECT COUNT(*) FROM consultation_session WHERE started_at >= #{ys} AND started_at < #{ye}) AS yoySessions,
              (SELECT COUNT(*) FROM emotion_diary) AS totalDiaries,
              (SELECT COUNT(*) FROM emotion_diary WHERE diary_date = #{td}) AS todayNewDiaries,
              (SELECT COUNT(*) FROM consultation_session) AS totalSessionsAll,
              (SELECT IFNULL(ROUND(AVG(mood_score), 1), 0) FROM emotion_diary) AS avgMoodScoreAll
            """)
    Map<String, Object> selectCoreMetrics(Map<String, Object> p);

    /**
     * 活跃用户（去重）单行，口径：范围内在 日记/会话/测评/预约 任一表有操作的去重用户数，
     * 同时返回本周期/上周期/去年同期三个值。依赖 assessment_record、appointment，缺失时由 Service 降级。
     */
    @Select("""
            SELECT
              (SELECT COUNT(DISTINCT user_id) FROM (
                 SELECT user_id FROM emotion_diary WHERE created_at >= #{rs} AND created_at < #{re}
                 UNION
                 SELECT user_id FROM consultation_session WHERE started_at >= #{rs} AND started_at < #{re}
                 UNION
                 SELECT user_id FROM assessment_record WHERE created_at >= #{rs} AND created_at < #{re}
                 UNION
                 SELECT user_id FROM appointment WHERE created_at >= #{rs} AND created_at < #{re}
              ) a) AS activeUsers,
              (SELECT COUNT(DISTINCT user_id) FROM (
                 SELECT user_id FROM emotion_diary WHERE created_at >= #{ps} AND created_at < #{pe}
                 UNION
                 SELECT user_id FROM consultation_session WHERE started_at >= #{ps} AND started_at < #{pe}
                 UNION
                 SELECT user_id FROM assessment_record WHERE created_at >= #{ps} AND created_at < #{pe}
                 UNION
                 SELECT user_id FROM appointment WHERE created_at >= #{ps} AND created_at < #{pe}
              ) b) AS prevActiveUsers,
              (SELECT COUNT(DISTINCT user_id) FROM (
                 SELECT user_id FROM emotion_diary WHERE created_at >= #{ys} AND created_at < #{ye}
                 UNION
                 SELECT user_id FROM consultation_session WHERE started_at >= #{ys} AND started_at < #{ye}
                 UNION
                 SELECT user_id FROM assessment_record WHERE created_at >= #{ys} AND created_at < #{ye}
                 UNION
                 SELECT user_id FROM appointment WHERE created_at >= #{ys} AND created_at < #{ye}
              ) c) AS yoyActiveUsers
            """)
    Map<String, Object> selectActiveUsers(Map<String, Object> p);

    /** 活跃用户降级查询：仅日记 + 会话（表始终存在），同样返回三周期计数 */
    @Select("""
            SELECT
              (SELECT COUNT(DISTINCT user_id) FROM (
                 SELECT user_id FROM emotion_diary WHERE created_at >= #{rs} AND created_at < #{re}
                 UNION
                 SELECT user_id FROM consultation_session WHERE started_at >= #{rs} AND started_at < #{re}
              ) a) AS activeUsers,
              (SELECT COUNT(DISTINCT user_id) FROM (
                 SELECT user_id FROM emotion_diary WHERE created_at >= #{ps} AND created_at < #{pe}
                 UNION
                 SELECT user_id FROM consultation_session WHERE started_at >= #{ps} AND started_at < #{pe}
              ) b) AS prevActiveUsers,
              (SELECT COUNT(DISTINCT user_id) FROM (
                 SELECT user_id FROM emotion_diary WHERE created_at >= #{ys} AND created_at < #{ye}
                 UNION
                 SELECT user_id FROM consultation_session WHERE started_at >= #{ys} AND started_at < #{ye}
              ) c) AS yoyActiveUsers
            """)
    Map<String, Object> selectActiveUsersBase(Map<String, Object> p);

    /** 测评完成数三周期计数（assessment_record，缺失时降级为 0） */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM assessment_record WHERE status = 'COMPLETED' AND completed_at >= #{rs} AND completed_at < #{re}) AS completed,
              (SELECT COUNT(*) FROM assessment_record WHERE status = 'COMPLETED' AND completed_at >= #{ps} AND completed_at < #{pe}) AS prevCompleted,
              (SELECT COUNT(*) FROM assessment_record WHERE status = 'COMPLETED' AND completed_at >= #{ys} AND completed_at < #{ye}) AS yoyCompleted
            """)
    Map<String, Object> selectAssessmentCounts(Map<String, Object> p);

    /** 预约数三周期计数（appointment，缺失时降级为 0） */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM appointment WHERE created_at >= #{rs} AND created_at < #{re}) AS total,
              (SELECT COUNT(*) FROM appointment WHERE created_at >= #{ps} AND created_at < #{pe}) AS prevTotal,
              (SELECT COUNT(*) FROM appointment WHERE created_at >= #{ys} AND created_at < #{ye}) AS yoyTotal
            """)
    Map<String, Object> selectAppointmentCounts(Map<String, Object> p);

    // ==================== 用户增长趋势 ====================

    /**
     * 新增用户趋势，fmt 由 Service 传入 '%Y-%m-%d'（day）或 '%Y-%m'（month），参数绑定防注入
     */
    @Select("""
            SELECT DATE_FORMAT(created_at, #{fmt}) AS label, COUNT(*) AS cnt
            FROM user
            WHERE created_at >= #{rs} AND created_at < #{re}
            GROUP BY label
            ORDER BY label
            """)
    List<Map<String, Object>> selectUserTrend(Map<String, Object> p);

    // ==================== 咨询统计 ====================

    /**
     * 会话每日趋势（补零由 Service 处理），同行带回范围内平均会话时长（分钟，标量子查询）：
     * 口径 = 会话最后一条消息时间 − started_at
     */
    @Select("""
            SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS date,
                   COUNT(*) AS sessionCount,
                   COUNT(DISTINCT user_id) AS userCount,
                   (SELECT IFNULL(ROUND(AVG(t.duration_minutes), 1), 0)
                    FROM (
                        SELECT TIMESTAMPDIFF(MINUTE, s2.started_at,
                               (SELECT MAX(m.created_at) FROM consultation_message m WHERE m.session_id = s2.id)) AS duration_minutes
                        FROM consultation_session s2
                        WHERE s2.started_at >= #{rs} AND s2.started_at < #{re}
                    ) t
                    WHERE t.duration_minutes IS NOT NULL AND t.duration_minutes >= 0) AS avgDurationMinutes
            FROM consultation_session
            WHERE started_at >= #{rs} AND started_at < #{re}
            GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
            ORDER BY date
            """)
    List<Map<String, Object>> selectSessionTrendWithAvg(Map<String, Object> p);

    /** 咨询主题分布（scene 列由 3.3 增量迁移添加，旧库缺失时降级为空） */
    @Select("""
            SELECT scene AS name, COUNT(*) AS value
            FROM consultation_session
            WHERE started_at >= #{rs} AND started_at < #{re} AND scene IS NOT NULL AND scene <> ''
            GROUP BY scene
            ORDER BY value DESC
            """)
    List<Map<String, Object>> selectSceneDistribution(Map<String, Object> p);

    /** 咨询消息情绪标签分布（JOIN 会话限定范围） */
    @Select("""
            SELECT m.emotion_tag AS name, COUNT(*) AS value
            FROM consultation_message m
              JOIN consultation_session s ON m.session_id = s.id
            WHERE s.started_at >= #{rs} AND s.started_at < #{re}
              AND m.emotion_tag IS NOT NULL AND m.emotion_tag <> ''
            GROUP BY m.emotion_tag
            ORDER BY value DESC
            """)
    List<Map<String, Object>> selectMessageEmotionDistribution(Map<String, Object> p);

    // ==================== 测评分析（assessment_record / assessment_scale） ====================

    /** 各量表完成次数 */
    @Select("""
            SELECT sc.name AS name, COUNT(*) AS value
            FROM assessment_record r
              JOIN assessment_scale sc ON r.scale_id = sc.id
            WHERE r.status = 'COMPLETED' AND r.completed_at >= #{rs} AND r.completed_at < #{re}
            GROUP BY sc.id, sc.name
            ORDER BY value DESC
            """)
    List<Map<String, Object>> selectScaleCounts(Map<String, Object> p);

    /** 结果等级分布 */
    @Select("""
            SELECT level AS name, COUNT(*) AS value
            FROM assessment_record
            WHERE status = 'COMPLETED' AND completed_at >= #{rs} AND completed_at < #{re}
              AND level IS NOT NULL AND level <> ''
            GROUP BY level
            """)
    List<Map<String, Object>> selectLevelCounts(Map<String, Object> p);

    /**
     * 高危用户预警 TOP10。高危口径：risk_flag = 1（风险标记，assessment_record 无 riskLevel 列，
     * risk_flag 为其落库映射）或 level = 'SEVERE'；按用户聚合，仅返回元数据，不含任何作答/报告内容。
     */
    @Select("""
            SELECT COALESCE(NULLIF(u.nickname, ''), u.username, '用户') AS nickname,
                   u.phone AS phone,
                   GROUP_CONCAT(DISTINCT r.level) AS levels,
                   MAX(r.completed_at) AS lastTime,
                   COUNT(*) AS cnt
            FROM assessment_record r
              JOIN user u ON r.user_id = u.id
            WHERE r.status = 'COMPLETED'
              AND r.completed_at >= #{rs} AND r.completed_at < #{re}
              AND (r.risk_flag = 1 OR r.level = 'SEVERE')
            GROUP BY u.id, u.nickname, u.phone
            ORDER BY lastTime DESC
            LIMIT 10
            """)
    List<Map<String, Object>> selectRiskUsers(Map<String, Object> p);

    // ==================== 情绪大数据（emotion_diary） ====================

    /** 范围内主导情绪分布 */
    @Select("""
            SELECT dominant_emotion AS name, COUNT(*) AS value
            FROM emotion_diary
            WHERE diary_date >= #{rs} AND diary_date < #{re}
              AND dominant_emotion IS NOT NULL AND dominant_emotion <> ''
            GROUP BY dominant_emotion
            ORDER BY value DESC
            """)
    List<Map<String, Object>> selectDiaryEmotionDistribution(Map<String, Object> p);

    /** 近30天 日期×主导情绪 明细（矩阵由 Service 组装），固定使用 h30s/tm 键 */
    @Select("""
            SELECT DATE_FORMAT(diary_date, '%Y-%m-%d') AS date,
                   dominant_emotion AS emotion,
                   COUNT(*) AS cnt
            FROM emotion_diary
            WHERE diary_date >= #{h30s} AND diary_date < #{tm}
              AND dominant_emotion IS NOT NULL AND dominant_emotion <> ''
            GROUP BY date, emotion
            """)
    List<Map<String, Object>> selectEmotionHeatmap(Map<String, Object> p);

    // ==================== 内容排行（与时间范围无关，TOP10） ====================

    /** 文章浏览量排行 */
    @Select("SELECT title AS name, read_count AS value FROM knowledge_article WHERE status = 1 ORDER BY read_count DESC LIMIT 10")
    List<Map<String, Object>> selectTopArticles();

    /** 课程学习量排行（course 缺失时降级为空） */
    @Select("SELECT title AS name, learn_count AS value FROM course WHERE status = 1 ORDER BY learn_count DESC LIMIT 10")
    List<Map<String, Object>> selectTopCourses();

    /** 热门帖子排行，权重 = 点赞数 + 评论数（community_post 缺失时降级为空），仅取正常帖 */
    @Select("""
            SELECT title AS name, (like_count + comment_count) AS value
            FROM community_post
            WHERE status = 0
            ORDER BY value DESC, id DESC
            LIMIT 10
            """)
    List<Map<String, Object>> selectTopPosts();

    // ==================== 预约统计（appointment / consultant） ====================

    /** 预约量每日趋势 */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS value
            FROM appointment
            WHERE created_at >= #{rs} AND created_at < #{re}
            GROUP BY date
            ORDER BY date
            """)
    List<Map<String, Object>> selectAppointmentDailyTrend(Map<String, Object> p);

    /** 预约完成率分子分母：COMPLETED / (总数 − CANCELED) */
    @Select("""
            SELECT COUNT(*) AS total,
                   SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
                   SUM(CASE WHEN status <> 'CANCELED' THEN 1 ELSE 0 END) AS effective
            FROM appointment
            WHERE created_at >= #{rs} AND created_at < #{re}
            """)
    Map<String, Object> selectAppointmentRate(Map<String, Object> p);

    /** 咨询师预约量排行 TOP10（按全部预约量计） */
    @Select("""
            SELECT c.name AS name, COUNT(a.id) AS value
            FROM appointment a
              JOIN consultant c ON a.consultant_id = c.id
            WHERE a.created_at >= #{rs} AND a.created_at < #{re}
            GROUP BY c.id, c.name
            ORDER BY value DESC, c.id ASC
            LIMIT 10
            """)
    List<Map<String, Object>> selectConsultantRanking(Map<String, Object> p);

    /** 预约各状态占比 */
    @Select("""
            SELECT status AS name, COUNT(*) AS value
            FROM appointment
            WHERE created_at >= #{rs} AND created_at < #{re}
            GROUP BY status
            """)
    List<Map<String, Object>> selectAppointmentStatusDist(Map<String, Object> p);

    /** 热门时段：按开始小时聚合（0-23） */
    @Select("""
            SELECT HOUR(start_time) AS name, COUNT(*) AS value
            FROM appointment
            WHERE created_at >= #{rs} AND created_at < #{re}
            GROUP BY HOUR(start_time)
            ORDER BY value DESC
            """)
    List<Map<String, Object>> selectAppointmentHotHours(Map<String, Object> p);

    // ==================== 旧版概览兼容序列（按范围参数化） ====================

    /** 情绪趋势：按日记日期聚合平均评分与记录数 */
    @Select("""
            SELECT DATE_FORMAT(diary_date, '%Y-%m-%d') AS date,
                   ROUND(AVG(mood_score), 1) AS avgMoodScore,
                   COUNT(*) AS recordCount
            FROM emotion_diary
            WHERE diary_date >= #{rs} AND diary_date < #{re}
            GROUP BY date
            ORDER BY date
            """)
    List<Map<String, Object>> selectEmotionTrend(Map<String, Object> p);

    /**
     * 用户活跃度联合序列（一次往返）：
     * type=new 每日新增用户 / diary 写日记用户 / consult 咨询用户 / active 去重活跃用户（日记+会话）
     */
    @Select("""
            SELECT type, date, cnt FROM (
                SELECT 'new' AS type, DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS cnt
                FROM user WHERE created_at >= #{rs} AND created_at < #{re}
                GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
                UNION ALL
                SELECT 'diary', DATE_FORMAT(created_at, '%Y-%m-%d'), COUNT(DISTINCT user_id)
                FROM emotion_diary WHERE created_at >= #{rs} AND created_at < #{re}
                GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
                UNION ALL
                SELECT 'consult', DATE_FORMAT(started_at, '%Y-%m-%d'), COUNT(DISTINCT user_id)
                FROM consultation_session WHERE started_at >= #{rs} AND started_at < #{re}
                GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
                UNION ALL
                SELECT 'active', t.date, COUNT(DISTINCT t.user_id) FROM (
                    SELECT user_id, DATE(created_at) AS date FROM emotion_diary
                    WHERE created_at >= #{rs} AND created_at < #{re}
                    UNION ALL
                    SELECT user_id, DATE(started_at) FROM consultation_session
                    WHERE started_at >= #{rs} AND started_at < #{re}
                ) t
                GROUP BY t.date
            ) x
            ORDER BY date
            """)
    List<Map<String, Object>> selectActivityUnion(Map<String, Object> p);
}
