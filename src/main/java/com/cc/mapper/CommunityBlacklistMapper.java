package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AdminBlacklistResponseDTO;
import com.cc.entity.CommunityBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 社区黑名单 Mapper
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface CommunityBlacklistMapper extends BaseMapper<CommunityBlacklist> {

    /**
     * 管理端黑名单分页：带用户昵称（昵称为空回退用户名）
     */
    @Select("""
            <script>
            SELECT b.id, b.user_id AS userId, b.reason, b.created_at AS createdAt,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS nickname
            FROM community_blacklist b
            LEFT JOIN user u ON u.id = b.user_id
            <where>
                <if test='keyword != null and keyword != ""'>
                    AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                         OR u.username LIKE CONCAT('%', #{keyword}, '%'))
                </if>
            </where>
            ORDER BY b.created_at DESC
            </script>
            """)
    IPage<AdminBlacklistResponseDTO> selectAdminPage(Page<AdminBlacklistResponseDTO> page,
                                                     @Param("keyword") String keyword);
}
