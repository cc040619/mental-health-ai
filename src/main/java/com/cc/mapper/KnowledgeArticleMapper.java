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
     * @param sortColumn    排序列（published_at/read_count/updated_at）
     * @param sortDirection 排序方向（ASC/DESC）
     * @return 分页结果
     */
    @Select("""
            <script>
            SELECT a.id, a.category_id, a.title, a.summary, a.cover_image, a.tags, a.read_count,
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
}
