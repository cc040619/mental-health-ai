package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/**
 * 心理课程 Mapper，继承 BaseMapper 获得基础 CRUD
 * <p>广场列表的 isLearning/isFavorite 由 Service 层批量查询后内存组装，避免 N+1
 *
 * @author cc
 * @date 2026-09-09
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
