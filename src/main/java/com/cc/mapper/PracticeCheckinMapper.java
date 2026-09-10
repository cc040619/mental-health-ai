package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.dto.response.CheckinCalendarResponseDTO;
import com.cc.entity.PracticeCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日练习打卡 Mapper，继承 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-09
 */
@Mapper
public interface PracticeCheckinMapper extends BaseMapper<PracticeCheckin> {

    /**
     * 查询某月打卡日历数据（关联课程/章节标题）
     *
     * @param userId 用户ID
     * @param start  月初
     * @param end    月末
     * @return 打卡记录列表
     */
    @Select("""
            SELECT pc.checkin_date AS checkinDate,
                   c.title AS courseTitle,
                   ch.title AS chapterTitle
            FROM practice_checkin pc
            LEFT JOIN course c ON c.id = pc.course_id
            LEFT JOIN course_chapter ch ON ch.id = pc.chapter_id
            WHERE pc.user_id = #{userId}
              AND pc.checkin_date >= #{start}
              AND pc.checkin_date <= #{end}
            ORDER BY pc.checkin_date ASC
            """)
    List<CheckinCalendarResponseDTO> selectMonthCheckins(@Param("userId") Long userId,
                                                         @Param("start") LocalDate start,
                                                         @Param("end") LocalDate end);
}
