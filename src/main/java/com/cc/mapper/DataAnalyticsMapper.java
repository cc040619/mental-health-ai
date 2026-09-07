package com.cc.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 数据分析聚合查询 Mapper，各统计项均为独立聚合 SQL，由 Service 层组装
 *
 * @author cc
 * @date 2026-09-06
 */
@Mapper
public interface DataAnalyticsMapper {

    /** 用户总数 */
    @Select("SELECT COUNT(*) FROM user")
    long countUsers();

    /** 活跃用户数：近7天有日记或咨询会话记录的独立用户数 */
    @Select("""
            SELECT COUNT(DISTINCT user_id) FROM (
                SELECT user_id FROM emotion_diary WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                UNION
                SELECT user_id FROM consultation_session WHERE started_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            ) t
            """)
    long countActiveUsers();

    /** 日记总数 */
    @Select("SELECT COUNT(*) FROM emotion_diary")
    long countDiaries();

    /** 今日新增日记数 */
    @Select("SELECT COUNT(*) FROM emotion_diary WHERE diary_date = CURDATE()")
    long countTodayNewDiaries();

    /** 会话总数 */
    @Select("SELECT COUNT(*) FROM consultation_session")
    long countSessions();

    /** 今日新增会话数 */
    @Select("SELECT COUNT(*) FROM consultation_session WHERE DATE(started_at) = CURDATE()")
    long countTodayNewSessions();

    /** 全部日记的平均情绪评分 */
    @Select("SELECT IFNULL(ROUND(AVG(mood_score), 1), 0) FROM emotion_diary")
    double avgMoodScore();

    /** 近7天情绪趋势：按日记日期聚合平均评分与记录数 */
    @Select("""
            SELECT DATE_FORMAT(diary_date, '%Y-%m-%d') AS date,
                   ROUND(AVG(mood_score), 1) AS avgMoodScore,
                   COUNT(*) AS recordCount
            FROM emotion_diary
            WHERE diary_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY diary_date
            ORDER BY diary_date
            """)
    List<Map<String, Object>> selectEmotionTrend();

    /** 近7天咨询每日趋势：按会话开始日期聚合会话数与参与用户数 */
    @Select("""
            SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS date,
                   COUNT(*) AS sessionCount,
                   COUNT(DISTINCT user_id) AS userCount
            FROM consultation_session
            WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
            ORDER BY DATE_FORMAT(started_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> selectConsultationDailyTrend();

    /** 平均咨询时长（分钟）：每条会话从开始到最后一条消息的时长的平均值 */
    @Select("""
            SELECT IFNULL(ROUND(AVG(t.duration_minutes), 1), 0) FROM (
                SELECT TIMESTAMPDIFF(MINUTE, s.started_at,
                       (SELECT MAX(m.created_at) FROM consultation_message m WHERE m.session_id = s.id)) AS duration_minutes
                FROM consultation_session s
            ) t
            WHERE t.duration_minutes IS NOT NULL AND t.duration_minutes >= 0
            """)
    double avgConsultationDurationMinutes();

    /** 近7天每日新增用户数 */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS cnt
            FROM user
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> selectDailyNewUsers();

    /** 近7天每日写日记的独立用户数 */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(DISTINCT user_id) AS cnt
            FROM emotion_diary
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> selectDailyDiaryUsers();

    /** 近7天每日咨询的独立用户数 */
    @Select("""
            SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS date, COUNT(DISTINCT user_id) AS cnt
            FROM consultation_session
            WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> selectDailyConsultationUsers();

    /** 近7天每日活跃用户数（有日记或咨询记录的独立用户数） */
    @Select("""
            SELECT t.date, COUNT(DISTINCT t.user_id) AS cnt FROM (
                SELECT user_id, DATE(created_at) AS date FROM emotion_diary
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                UNION ALL
                SELECT user_id, DATE(started_at) AS date FROM consultation_session
                WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            ) t
            GROUP BY t.date
            ORDER BY t.date
            """)
    List<Map<String, Object>> selectDailyActiveUsers();
}
