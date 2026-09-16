package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.dto.response.AdminBoardResponseDTO;
import com.cc.entity.CommunityBoardEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 社区板块 Mapper
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface CommunityBoardMapper extends BaseMapper<CommunityBoardEntity> {

    /**
     * 管理端板块列表（全量含停用）：帖子数用相关子查询单条聚合（非 N+1，板块数量个位数）
     */
    @Select("""
            SELECT b.id, b.code, b.name, b.icon, b.description, b.sort_order AS sortOrder,
                   b.moderator_id AS moderatorId, b.status, b.created_at AS createdAt,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS moderatorName,
                   (SELECT COUNT(*) FROM community_post p
                     WHERE p.board = b.code AND p.status != 1) AS postCount
            FROM community_board b
            LEFT JOIN user u ON u.id = b.moderator_id
            ORDER BY b.sort_order ASC, b.id ASC
            """)
    List<AdminBoardResponseDTO> selectAdminBoardList();
}
