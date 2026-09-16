package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminCourseListResponseDTO;
import com.cc.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 心理课程 Mapper，继承 BaseMapper 获得基础 CRUD
 * <p>广场列表的 isLearning/isFavorite 由 Service 层批量查询后内存组装，避免 N+1
 *
 * @author cc
 * @date 2026-09-09
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 管理端列表分页：LEFT JOIN 章节表 GROUP BY 一次算出 章节数/总时长，禁止 N+1
     * <p>keyword 匹配标题或讲师（中缀 LIKE），status 五态筛，category 五类筛。
     * 命中 idx_status_created 索引。total_duration 取 SUM 聚合结果（最准确），
     * 与 course.total_duration 冗余列由 recomputeTotalDuration 保持同步。</p>
     *
     * @param page     分页参数
     * @param keyword  关键字（标题或讲师），可空
     * @param category 类别 code，可空
     * @param status   状态，可空
     */
    @Select("""
            <script>
            SELECT c.id, c.title, c.category, c.cover_url, c.lecturer,
                   c.learn_count, c.status, c.reject_reason,
                   c.created_at, c.published_at,
                   COUNT(ch.id) AS chapter_count,
                   COALESCE(SUM(ch.duration), 0) AS total_duration
            FROM course c
            LEFT JOIN course_chapter ch ON ch.course_id = c.id
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (c.title LIKE CONCAT('%', #{keyword}, '%')
                         OR c.lecturer LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="category != null and category != ''">
                    AND c.category = #{category}
                </if>
                <if test="status != null">
                    AND c.status = #{status}
                </if>
            </where>
            GROUP BY c.id
            ORDER BY c.created_at DESC
            </script>
            """)
    IPage<AdminCourseListResponseDTO> selectAdminPage(Page<AdminCourseListResponseDTO> page,
                                                      @Param("keyword") String keyword,
                                                      @Param("category") String category,
                                                      @Param("status") Integer status);

    /**
     * 各状态课程计数（前端状态 Tab 展示数字），单次 GROUP BY 全状态
     */
    @Select("""
            SELECT status, COUNT(*) AS cnt
            FROM course
            GROUP BY status
            """)
    java.util.List<java.util.Map<String, Object>> selectStatusCount();
}
