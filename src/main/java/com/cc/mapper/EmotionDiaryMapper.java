package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.EmotionDiaryPageQueryDTO;
import com.cc.dto.response.EmotionDiaryResponseDTO;
import com.cc.entity.EmotionDiary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 情绪日记表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-06
 */
@Mapper
public interface EmotionDiaryMapper extends BaseMapper<EmotionDiary> {

    /**
     * 后台分页查询日记列表，关联用户表返回昵称与用户名
     *
     * @param page  分页参数
     * @param query 查询条件（用户ID、情绪评分范围）
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT d.*, u.nickname, u.username
            FROM emotion_diary d
            LEFT JOIN user u ON u.id = d.user_id
            <where>
                <if test="q.userId != null">AND d.user_id = #{q.userId}</if>
                <if test="q.moodScoreMin != null">AND d.mood_score &gt;= #{q.moodScoreMin}</if>
                <if test="q.moodScoreMax != null">AND d.mood_score &lt;= #{q.moodScoreMax}</if>
            </where>
            ORDER BY d.created_at DESC
            </script>
            """)
    IPage<EmotionDiaryResponseDTO> selectAdminPage(Page<EmotionDiaryResponseDTO> page,
                                                   @Param("q") EmotionDiaryPageQueryDTO query);
}
