package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.PushRecordResponseDTO;
import com.cc.entity.NotificationPush;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 消息推送批次表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface NotificationPushMapper extends BaseMapper<NotificationPush> {

    /**
     * 推送记录分页：LEFT JOIN notification 一次聚合已读数（related_type=PUSH、related_id=批次ID），
     * 阅读数实时统计不落冗余，禁止 N+1
     */
    @Select("""
            <script>
            SELECT p.id, p.type, p.title, p.content,
                   p.target_type AS targetType, p.target_count AS targetCount, p.created_at AS createdAt,
                   COALESCE(SUM(CASE WHEN n.is_read = 1 THEN 1 ELSE 0 END), 0) AS readCount
            FROM notification_push p
            LEFT JOIN notification n ON n.related_id = p.id AND n.related_type = 'PUSH'
            GROUP BY p.id
            ORDER BY p.created_at DESC
            </script>
            """)
    IPage<PushRecordResponseDTO> selectPushPage(Page<PushRecordResponseDTO> page);
}
