package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CourseProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 章节学习进度 Mapper，继承 BaseMapper 获得基础 CRUD
 * <p>upsert 由 Service 层基于唯一键(user_id+chapter_id) select 后 insert/updateById 实现
 *
 * @author cc
 * @date 2026-09-09
 */
@Mapper
public interface CourseProgressMapper extends BaseMapper<CourseProgress> {

    /**
     * 批量查询当前用户已开始学习（有进度记录）的课程ID
     * <p>用于广场列表批量组装 isLearning，避免 N+1
     */
    @Select("""
            <script>
            SELECT DISTINCT ch.course_id
            FROM course_progress cp
            INNER JOIN course_chapter ch ON ch.id = cp.chapter_id
            WHERE cp.user_id = #{userId}
            <if test="courseIds != null and courseIds.size() > 0">
                AND ch.course_id IN
                <foreach collection="courseIds" item="cid" open="(" close=")" separator=",">#{cid}</foreach>
            </if>
            </script>
            """)
    List<Long> selectLearnedCourseIds(@Param("userId") Long userId,
                                      @Param("courseIds") List<Long> courseIds);

    /**
     * 课程完成率分子：学完全部章节的用户数
     * <p>口径：对 course_progress 按 user_id 分组，HAVING SUM(is_finished) = 该课程章节数，
     * 即用户已完成所有章节。一次 HAVING 聚合，禁止逐用户循环查询。</p>
     * <p>ponytail: 数据量小可接受全表 GROUP BY；百万级学习记录时可改为物化视图或定时聚合表。</p>
     *
     * @param courseId        课程ID
     * @param totalChapterCnt 该课程总章节数
     * @return 完成全部章节的用户数
     */
    @Select("""
            SELECT COUNT(*) FROM (
                SELECT cp.user_id, SUM(cp.is_finished) AS finished_cnt
                FROM course_progress cp
                INNER JOIN course_chapter ch ON ch.id = cp.chapter_id
                WHERE ch.course_id = #{courseId}
                GROUP BY cp.user_id
                HAVING SUM(cp.is_finished) >= #{totalChapterCnt}
            ) t
            """)
    Integer selectCompletedUserCount(@Param("courseId") Long courseId,
                                     @Param("totalChapterCnt") Integer totalChapterCnt);

    /**
     * 平均学习时长 = SUM(progress_seconds) / COUNT(DISTINCT user_id)
     * <p>仅统计该课程的进度记录，单 SQL 聚合</p>
     */
    @Select("""
            SELECT COALESCE(SUM(cp.progress_seconds) / NULLIF(COUNT(DISTINCT cp.user_id), 0), 0)
            FROM course_progress cp
            INNER JOIN course_chapter ch ON ch.id = cp.chapter_id
            WHERE ch.course_id = #{courseId}
            """)
    Integer selectAvgProgressSeconds(@Param("courseId") Long courseId);

    /**
     * 课程学习人数：有任意章节进度记录的去重用户数
     * <p>对应「我的课程-正在学习」中有该课程的用户数，优先于冗余 learn_count</p>
     */
    @Select("""
            SELECT COUNT(DISTINCT cp.user_id)
            FROM course_progress cp
            INNER JOIN course_chapter ch ON ch.id = cp.chapter_id
            WHERE ch.course_id = #{courseId}
            """)
    Integer selectLearnerCount(@Param("courseId") Long courseId);

    /**
     * 章节学习热力图：每章 {学习人数, 完成人数, 平均进度}
     * <p>一次 JOIN 聚合，禁止逐章循环查询</p>
     *
     * @param courseId 课程ID
     * @return 每行包含 chapter_id, title, sort_order, learner_count, finish_count, avg_progress
     */
    @Select("""
            SELECT ch.id AS chapter_id,
                   ch.title AS title,
                   ch.sort_order AS sort_order,
                   COUNT(cp.id) AS learner_count,
                   COALESCE(SUM(cp.is_finished), 0) AS finish_count,
                   COALESCE(AVG(cp.progress_seconds), 0) AS avg_progress
            FROM course_chapter ch
            LEFT JOIN course_progress cp ON cp.chapter_id = ch.id
            WHERE ch.course_id = #{courseId}
            GROUP BY ch.id, ch.title, ch.sort_order
            ORDER BY ch.sort_order ASC, ch.id ASC
            """)
    List<Map<String, Object>> selectChapterHeatmap(@Param("courseId") Long courseId);
}
