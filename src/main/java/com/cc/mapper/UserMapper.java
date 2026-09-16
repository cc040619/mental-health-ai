package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 性别分布聚合：按 gender 分组，gender 为 null/0 在 Service 归为「未知」
     */
    @Select("""
            SELECT IFNULL(gender, 0) AS gender, COUNT(*) AS cnt
            FROM user
            GROUP BY IFNULL(gender, 0)
            """)
    List<Map<String, Object>> selectGenderDistribution();

    /**
     * 年龄段分布聚合：按 TIMESTAMPDIFF 计算年龄并分档，无生日归「未知」
     */
    @Select("""
            SELECT
              CASE
                WHEN birthday IS NULL THEN '未知'
                WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) <= 18 THEN '0-18'
                WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) <= 25 THEN '19-25'
                WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) <= 35 THEN '26-35'
                WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) <= 50 THEN '36-50'
                ELSE '50+'
              END AS label,
              COUNT(*) AS cnt
            FROM user
            GROUP BY label
            """)
    List<Map<String, Object>> selectAgeDistribution();

    /**
     * 注册趋势：近 30 天每日新增用户数（含首尾）；Service 层按日期补零
     */
    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS cnt
            FROM user
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> selectRegistrationTrend();
}
