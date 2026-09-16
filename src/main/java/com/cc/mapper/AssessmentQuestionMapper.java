package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.entity.AssessmentQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 通用题目库 Mapper
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface AssessmentQuestionMapper extends BaseMapper<AssessmentQuestion> {

    /**
     * 题目库分页：keyword 匹配题干，type 筛题型
     */
    @Select("""
            <script>
            SELECT * FROM assessment_question
            <where>
                <if test="keyword != null and keyword != ''">
                    AND stem LIKE CONCAT('%', #{keyword}, '%')
                </if>
                <if test="type != null and type != ''">
                    AND type = #{type}
                </if>
            </where>
            ORDER BY created_at DESC
            </script>
            """)
    IPage<AssessmentQuestion> selectAdminPage(Page<AssessmentQuestion> page,
                                              @Param("keyword") String keyword,
                                              @Param("type") String type);
}
