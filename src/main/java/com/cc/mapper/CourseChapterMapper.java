package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CourseChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 课程章节 Mapper，继承 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-09
 */
@Mapper
public interface CourseChapterMapper extends BaseMapper<CourseChapter> {

    /**
     * 按课程ID查询全部章节（按 sort_order 升序）
     * <p>详情页一次性查出该课程全部章节，避免逐章查询
     */
    @Select("SELECT * FROM course_chapter WHERE course_id = #{courseId} ORDER BY sort_order ASC, id ASC")
    List<CourseChapter> selectByCourseId(@Param("courseId") Long courseId);

    /**
     * 取当前课程最大 sort_order，新增章节时使用
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM course_chapter WHERE course_id = #{courseId}")
    Integer selectMaxSortOrder(@Param("courseId") Long courseId);

    /**
     * 章节总时长聚合：SUM(duration)，重算 totalDuration 时调用
     */
    @Select("SELECT COALESCE(SUM(duration), 0) FROM course_chapter WHERE course_id = #{courseId}")
    Integer selectTotalDuration(@Param("courseId") Long courseId);

    /**
     * 批量更新 sortOrder（单事务，逐条 UPDATE 由 MyBatis 循环执行）
     * <p>ponytail: 章节量级小（单课最多几十章节），逐条 UPDATE 可接受；
     * 章节量级上来时改用 CASE WHEN 单 SQL 批量更新。
     */
    @Update("UPDATE course_chapter SET sort_order = #{sortOrder} WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
}
