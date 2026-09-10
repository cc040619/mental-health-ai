package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CommunityFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 社区收藏 Mapper
 *
 * @author cc
 * @date 2026-09-10
 */
@Mapper
public interface CommunityFavoriteMapper extends BaseMapper<CommunityFavorite> {

    /**
     * 查询当前用户已收藏的帖子ID集合（批量补齐列表互动状态）
     */
    @Select("""
            <script>
            SELECT post_id FROM community_favorite
            WHERE user_id = #{userId}
            AND post_id IN
            <foreach collection='postIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    List<Long> selectFavoritedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
