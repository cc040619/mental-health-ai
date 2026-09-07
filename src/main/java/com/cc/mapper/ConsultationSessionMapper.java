package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.ConsultationSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 咨询会话表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-01
 */
@Mapper
public interface ConsultationSessionMapper extends BaseMapper<ConsultationSession> {

    /**
     * 按指定ID插入会话（用于流式对话时按原ID重建已删除的会话，
     * 保证前端未刷新页面时当前对话可无缝继续）
     *
     * @return 受影响行数（0表示该ID已被并发请求占用）
     */
    @Insert("INSERT INTO consultation_session (id, user_id, session_title, started_at) " +
            "VALUES (#{id}, #{userId}, #{sessionTitle}, #{startedAt})")
    int insertWithId(ConsultationSession session);
}
