package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminCourseCommentResponseDTO;
import com.cc.dto.response.CourseCommentResponseDTO;
import com.cc.entity.CourseComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课程评论 Mapper，继承 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-09
 */
@Mapper
public interface CourseCommentMapper extends BaseMapper<CourseComment> {

    /**
     * 用户端分页查询课程评论，关联 user 表返回昵称/头像，并标记是否本人
     *
     * @param page     分页参数
     * @param courseId 课程ID
     * @param userId   当前登录用户ID（用于 isMine 标记）
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT c.id, c.course_id, c.user_id, c.content, c.created_at,
                   u.nickname, u.avatar,
                   CASE WHEN c.user_id = #{userId} THEN 1 ELSE 0 END AS is_mine,
                   c.reply_content, c.reply_at
            FROM course_comment c
            LEFT JOIN user u ON u.id = c.user_id
            WHERE c.course_id = #{courseId}
            ORDER BY c.created_at DESC
            </script>
            """)
    IPage<CourseCommentResponseDTO> selectCommentPage(Page<CourseCommentResponseDTO> page,
                                                     @Param("courseId") Long courseId,
                                                     @Param("userId") Long userId);

    /**
     * 管理端评论分页：关联 user + course 表，keyword 中缀匹配评论内容
     * <p>命中 idx_course_created；数据量小 LIKE 中缀可接受</p>
     *
     * @param page     分页参数
     * @param courseId 课程ID（可空，按课程筛选）
     * @param keyword  关键字（匹配评论内容），可空
     */
    @Select("""
            <script>
            SELECT c.id, c.course_id, c.user_id, c.content, c.reply_content, c.reply_at,
                   c.created_at,
                   co.title AS course_title,
                   u.nickname, u.avatar
            FROM course_comment c
            LEFT JOIN course co ON co.id = c.course_id
            LEFT JOIN user u ON u.id = c.user_id
            <where>
                <if test="courseId != null">
                    AND c.course_id = #{courseId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND c.content LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            ORDER BY c.created_at DESC
            </script>
            """)
    IPage<AdminCourseCommentResponseDTO> selectAdminPage(Page<AdminCourseCommentResponseDTO> page,
                                                        @Param("courseId") Long courseId,
                                                        @Param("keyword") String keyword);
}
