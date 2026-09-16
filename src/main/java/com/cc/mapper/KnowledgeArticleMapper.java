package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ArticlePageQueryDTO;
import com.cc.dto.response.ArticleResponseDTO;
import com.cc.dto.response.ArticleSimpleResponseDTO;
import com.cc.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 知识文章表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-06
 */
@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {

    /**
     * 分页查询文章列表，关联分类表与用户表返回分类名称与作者名称
     *
     * <p>sortColumn/sortDirection 由 Service 层白名单校验后传入，防止 SQL 注入。</p>
     *
     * @param page          分页参数
     * @param query         查询条件（标题、分类、状态）
     * @param sortColumn    排序列（published_at/read_count/like_count/comment_count/favorite_count/updated_at）
     * @param sortDirection 排序方向（ASC/DESC）
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT a.id, a.category_id, a.title, a.summary, a.cover_image, a.tags, a.read_count,
                   a.like_count, a.comment_count, a.favorite_count,
                   a.status, a.published_at, a.created_at, a.updated_at,
                   c.category_name, u.nickname AS author_name
            FROM knowledge_article a
            LEFT JOIN knowledge_category c ON c.id = a.category_id
            LEFT JOIN user u ON u.id = a.author_id
            <where>
                <if test="q.title != null and q.title != ''">AND a.title LIKE CONCAT('%', #{q.title}, '%')</if>
                <if test="q.categoryId != null">AND a.category_id = #{q.categoryId}</if>
                <if test="q.status != null">AND a.status = #{q.status}</if>
            </where>
            ORDER BY ${sortColumn} ${sortDirection}, a.created_at DESC
            </script>
            """)
    IPage<ArticleSimpleResponseDTO> selectPageWithRelation(Page<ArticleSimpleResponseDTO> page,
                                                           @Param("q") ArticlePageQueryDTO query,
                                                           @Param("sortColumn") String sortColumn,
                                                           @Param("sortDirection") String sortDirection);

    /**
     * 查询文章详情，关联分类表与用户表返回分类名称与作者名称
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @Select("""
            SELECT a.*, c.category_name, u.nickname AS author_name
            FROM knowledge_article a
            LEFT JOIN knowledge_category c ON c.id = a.category_id
            LEFT JOIN user u ON u.id = a.author_id
            WHERE a.id = #{id}
            """)
    ArticleResponseDTO selectDetailById(@Param("id") String id);

    /**
     * 批量更新文章状态（上架/下架），单条 UPDATE ... IN
     *
     * @param ids    文章ID列表
     * @param status 目标状态 1:已发布 2:已下线
     * @return 受影响行数
     */
    @Update("""
            <script>
            UPDATE knowledge_article
            SET status = #{status}, updated_at = NOW()
            <if test="status == 1">, published_at = COALESCE(published_at, NOW())</if>
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    int batchUpdateStatus(@Param("ids") List<String> ids, @Param("status") Integer status);

    /**
     * 批量移动文章分类，单条 UPDATE ... IN
     *
     * @param ids        文章ID列表
     * @param categoryId 目标分类ID
     * @return 受影响行数
     */
    @Update("""
            <script>
            UPDATE knowledge_article
            SET category_id = #{categoryId}, updated_at = NOW()
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    int batchUpdateCategory(@Param("ids") List<String> ids, @Param("categoryId") Long categoryId);

    /**
     * 计数自增/自减（like_count/comment_count/favorite_count），事务内调用
     *
     * @param id    文章ID
     * @param column 合法列名（Service 白名单校验后传入）
     * @param delta +1 或 -1
     * @return 受影响行数
     */
    @Update("UPDATE knowledge_article SET ${column} = GREATEST(${column} + #{delta}, 0) WHERE id = #{id}")
    int incrementCount(@Param("id") String id, @Param("column") String column, @Param("delta") int delta);
}
