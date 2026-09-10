package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CourseProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}
