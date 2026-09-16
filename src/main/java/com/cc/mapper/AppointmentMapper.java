package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AppointmentResponseDTO;
import com.cc.dto.response.ConsultantSessionResponseDTO;
import com.cc.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 咨询预约 Mapper：单表 CRUD 走 BaseMapper；
 * 多表分页（我的预约/后台预约列表）用 &lt;script&gt; 动态 SQL + DTO 投影。
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /**
     * 我的预约分页：关联咨询师卡片信息
     */
    @Select("""
            <script>
            SELECT a.id, a.user_id AS userId, a.consultant_id AS consultantId, a.slot_id AS slotId,
                   a.appointment_date AS appointmentDate,
                   DATE_FORMAT(a.start_time, '%H:%i') AS startTime,
                   DATE_FORMAT(a.end_time, '%H:%i') AS endTime,
                   a.problem_desc AS problemDesc, a.cancel_reason AS cancelReason,
                   a.status, a.evaluated, a.created_at AS createdAt,
                   c.name AS consultantName, c.avatar AS consultantAvatar, c.title AS consultantTitle
            FROM appointment a
            INNER JOIN consultant c ON c.id = a.consultant_id
            WHERE a.user_id = #{userId}
            <if test='status != null and status != ""'>
                AND a.status = #{status}
            </if>
            ORDER BY a.appointment_date DESC, a.start_time DESC, a.id DESC
            </script>
            """)
    IPage<AppointmentResponseDTO> selectMinePage(Page<AppointmentResponseDTO> page,
                                                 @Param("userId") Long userId,
                                                 @Param("status") String status);

    /**
     * 后台预约管理分页：关联咨询师 + 预约用户信息
     * 支持状态/咨询师/用户昵称关键字/预约日期范围筛选
     */
    @Select("""
            <script>
            SELECT a.id, a.user_id AS userId, a.consultant_id AS consultantId, a.slot_id AS slotId,
                   a.appointment_date AS appointmentDate,
                   DATE_FORMAT(a.start_time, '%H:%i') AS startTime,
                   DATE_FORMAT(a.end_time, '%H:%i') AS endTime,
                   a.problem_desc AS problemDesc, a.cancel_reason AS cancelReason,
                   a.status, a.evaluated, a.created_at AS createdAt,
                   c.name AS consultantName, c.avatar AS consultantAvatar, c.title AS consultantTitle,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS userNickname,
                   u.phone AS userPhone
            FROM appointment a
            INNER JOIN consultant c ON c.id = a.consultant_id
            INNER JOIN user u ON u.id = a.user_id
            <where>
                <if test='status != null and status != ""'>
                    AND a.status = #{status}
                </if>
                <if test='consultantId != null'>
                    AND a.consultant_id = #{consultantId}
                </if>
                <if test='keyword != null and keyword != ""'>
                    AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                         OR u.username LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test='startDate != null and startDate != ""'>
                    AND a.appointment_date <![CDATA[>=]]> #{startDate}
                </if>
                <if test='endDate != null and endDate != ""'>
                    AND a.appointment_date <![CDATA[<=]]> #{endDate}
                </if>
            </where>
            ORDER BY a.appointment_date DESC, a.start_time DESC, a.id DESC
            </script>
            """)
    IPage<AppointmentResponseDTO> selectAdminPage(Page<AppointmentResponseDTO> page,
                                                  @Param("status") String status,
                                                  @Param("consultantId") Long consultantId,
                                                  @Param("keyword") String keyword,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);

    /**
     * 用户管理 - 按指定用户ID分页查询其全部预约记录（管理员视角，含全部状态）
     */
    @Select("""
            <script>
            SELECT a.id, a.user_id AS userId, a.consultant_id AS consultantId, a.slot_id AS slotId,
                   a.appointment_date AS appointmentDate,
                   DATE_FORMAT(a.start_time, '%H:%i') AS startTime,
                   DATE_FORMAT(a.end_time, '%H:%i') AS endTime,
                   a.problem_desc AS problemDesc, a.status, a.evaluated, a.created_at AS createdAt,
                   c.name AS consultantName, c.avatar AS consultantAvatar, c.title AS consultantTitle
            FROM appointment a
            INNER JOIN consultant c ON c.id = a.consultant_id
            WHERE a.user_id = #{userId}
            ORDER BY a.appointment_date DESC, a.start_time DESC, a.id DESC
            </script>
            """)
    IPage<AppointmentResponseDTO> selectAdminByUserIdPage(Page<AppointmentResponseDTO> page,
                                                           @Param("userId") Long userId);

    /**
     * 咨询师管理 - 按指定咨询师ID分页查询其全部预约记录（管理员视角，含预约用户昵称）
     */
    @Select("""
            <script>
            SELECT a.id, a.user_id AS userId, a.consultant_id AS consultantId, a.slot_id AS slotId,
                   a.appointment_date AS appointmentDate,
                   DATE_FORMAT(a.start_time, '%H:%i') AS startTime,
                   DATE_FORMAT(a.end_time, '%H:%i') AS endTime,
                   a.problem_desc AS problemDesc, a.status, a.evaluated, a.created_at AS createdAt,
                   c.name AS consultantName, c.avatar AS consultantAvatar, c.title AS consultantTitle,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS userNickname
            FROM appointment a
            INNER JOIN consultant c ON c.id = a.consultant_id
            INNER JOIN user u ON u.id = a.user_id
            WHERE a.consultant_id = #{consultantId}
            ORDER BY a.appointment_date DESC, a.start_time DESC, a.id DESC
            </script>
            """)
    IPage<AppointmentResponseDTO> selectAdminByConsultantIdPage(Page<AppointmentResponseDTO> page,
                                                                @Param("consultantId") Long consultantId);

    /**
     * 咨询师端 - 我的预约分页：强制按 consultantId 过滤（Service 传当前登录咨询师 id），
     * 支持状态/预约日期筛选。隐私边界：只投影昵称，禁止 SELECT 手机号/邮箱。
     */
    @Select("""
            <script>
            SELECT a.id, a.user_id AS userId, a.consultant_id AS consultantId, a.slot_id AS slotId,
                   a.appointment_date AS appointmentDate,
                   DATE_FORMAT(a.start_time, '%H:%i') AS startTime,
                   DATE_FORMAT(a.end_time, '%H:%i') AS endTime,
                   a.problem_desc AS problemDesc, a.cancel_reason AS cancelReason,
                   a.status, a.evaluated, a.created_at AS createdAt,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS userNickname
            FROM appointment a
            INNER JOIN user u ON u.id = a.user_id
            WHERE a.consultant_id = #{consultantId}
            <if test='status != null and status != ""'>
                AND a.status = #{status}
            </if>
            <if test='date != null and date != ""'>
                AND a.appointment_date = #{date}
            </if>
            ORDER BY a.appointment_date DESC, a.start_time DESC, a.id DESC
            </script>
            """)
    IPage<AppointmentResponseDTO> selectConsultantPage(Page<AppointmentResponseDTO> page,
                                                       @Param("consultantId") Long consultantId,
                                                       @Param("status") String status,
                                                       @Param("date") String date);

    /**
     * 咨询师端「会话列表」分页：按当前咨询师 id 过滤，LEFT JOIN 聚合最后一条留言与留言数。
     * 一次查询完成，禁止 N+1。隐私边界：只投影昵称，禁止 SELECT 手机号/邮箱。
     * 排序：有最后留言的按最后留言时间倒序，无留言的按预约创建时间倒序。
     */
    @Select("""
            <script>
            SELECT a.id AS appointmentId, a.user_id AS userId, a.appointment_date AS appointmentDate,
                   DATE_FORMAT(a.start_time, '%H:%i') AS startTime,
                   DATE_FORMAT(a.end_time, '%H:%i') AS endTime,
                   a.problem_desc AS problemDesc, a.status, a.created_at AS createdAt,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS userNickname,
                   lm.content AS lastMessageContent, lm.created_at AS lastMessageTime,
                   COALESCE(mc.cnt, 0) AS messageCount
            FROM appointment a
            INNER JOIN user u ON u.id = a.user_id
            LEFT JOIN (
                SELECT m1.appointment_id, m1.content, m1.created_at
                FROM appointment_message m1
                INNER JOIN (
                    SELECT appointment_id, MAX(id) AS max_id
                    FROM appointment_message GROUP BY appointment_id
                ) m2 ON m1.id = m2.max_id
            ) lm ON lm.appointment_id = a.id
            LEFT JOIN (
                SELECT appointment_id, COUNT(*) AS cnt
                FROM appointment_message GROUP BY appointment_id
            ) mc ON mc.appointment_id = a.id
            WHERE a.consultant_id = #{consultantId}
            <if test='status != null and status != ""'>
                AND a.status = #{status}
            </if>
            <if test='keyword != null and keyword != ""'>
                AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                     OR u.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY COALESCE(lm.created_at, a.created_at) DESC, a.id DESC
            </script>
            """)
    IPage<ConsultantSessionResponseDTO> selectConsultantSessions(
            Page<ConsultantSessionResponseDTO> page,
            @Param("consultantId") Long consultantId,
            @Param("status") String status,
            @Param("keyword") String keyword);
}
