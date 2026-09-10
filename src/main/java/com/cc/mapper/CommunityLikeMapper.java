package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.CommunityLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 社区点赞 Mapper
 *
 * @author cc
 * @date 2026-09-10
 */
@Mapper
public interface CommunityLikeMapper extends BaseMapper<CommunityLike> {

    /**
     * 查询当前用户已点赞的帖子ID集合（批量补齐列表互动状态）
     */
    @Select("""
            <script>
            SELECT post_id FROM community_like
            WHERE user_id = #{userId}
            AND post_id IN
            <foreach collection='postIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    List<Long> selectLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
