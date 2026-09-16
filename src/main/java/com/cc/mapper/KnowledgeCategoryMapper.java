package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.dto.response.CategoryWithCountResponseDTO;
import com.cc.entity.KnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识分类表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-06
 */
@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {

    /**
     * 查询全部分类（含文章数量统计，管理端使用），按 sortOrder asc 排序
     * <p>LEFT JOIN 聚合避免 N+1 查询</p>
     *
     * @return 分类列表（含 articleCount）
     */
    @Select("""
            SELECT c.id, c.parent_id AS parentId, c.category_name AS categoryName,
                   c.category_code AS categoryCode, c.description, c.icon,
                   c.sort_order AS sortOrder, c.status,
                   COUNT(a.id) AS articleCount
            FROM knowledge_category c
            LEFT JOIN knowledge_article a ON a.category_id = c.id
            GROUP BY c.id
            ORDER BY c.sort_order ASC, c.id ASC
            """)
    List<CategoryWithCountResponseDTO> selectAllWithCount();
}
