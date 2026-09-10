package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CourseChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
