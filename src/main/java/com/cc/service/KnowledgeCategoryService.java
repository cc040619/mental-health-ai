package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.dto.response.CategoryResponseDTO;
import com.cc.entity.KnowledgeCategory;
import com.cc.mapper.KnowledgeCategoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识分类业务层，提供分类列表查询
 *
 * @author cc
 * @date 2026-09-06
 */
@Service
public class KnowledgeCategoryService {

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    /**
     * 查询启用状态的分类列表（按排序字段升序）
     *
     * @return 分类列表
     */
    public List<CategoryResponseDTO> listEnabled() {
        LambdaQueryWrapper<KnowledgeCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeCategory::getStatus, 1)
                .orderByAsc(KnowledgeCategory::getSortOrder)
                .orderByAsc(KnowledgeCategory::getId);

        return knowledgeCategoryMapper.selectList(queryWrapper).stream()
                .map(category -> CategoryResponseDTO.builder()
                        .id(category.getId())
                        .parentId(category.getParentId())
                        .categoryName(category.getCategoryName())
                        .description(category.getDescription())
                        .sortOrder(category.getSortOrder())
                        .build())
                .toList();
    }
}
