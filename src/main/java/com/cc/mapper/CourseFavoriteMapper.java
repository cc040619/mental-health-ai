package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.CourseListResponseDTO;
import com.cc.entity.CourseFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课程收藏 Mapper，继承 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-09
 */
@Mapper
public interface CourseFavoriteMapper extends BaseMapper<CourseFavorite> {

    /**
     * 批量查询当前用户已收藏的课程ID（用于广场列表批量组装 isFavorite，避免 N+1）
     */
    @Select("""
            <script>
            SELECT course_id FROM course_favorite
            WHERE user_id = #{userId}
            <if test="courseIds != null and courseIds.size() > 0">
                AND course_id IN
                <foreach collection="courseIds" item="cid" open="(" close=")" separator=",">#{cid}</foreach>
            </if>
            </script>
            """)
    List<Long> selectFavoriteCourseIds(@Param("userId") Long userId,
                                       @Param("courseIds") List<Long> courseIds);

    /**
     * 分页查询用户收藏的课程（关联 course 表，仅上架），返回列表项 DTO
     */
    @Select("""
            SELECT c.id, c.title, c.category, c.cover_url, c.total_duration, c.learn_count
            FROM course_favorite f
            INNER JOIN course c ON c.id = f.course_id
            WHERE f.user_id = #{userId} AND c.status = 1
            ORDER BY f.created_at DESC
            """)
    IPage<CourseListResponseDTO> selectFavoritesPage(Page<CourseListResponseDTO> page,
                                                    @Param("userId") Long userId);
}
