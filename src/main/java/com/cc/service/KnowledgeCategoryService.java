package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.dto.request.CategorySaveDTO;
import com.cc.dto.response.CategoryResponseDTO;
import com.cc.dto.response.CategoryWithCountResponseDTO;
import com.cc.entity.KnowledgeCategory;
import com.cc.exception.BusinessException;
import com.cc.mapper.KnowledgeCategoryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识分类业务层，提供分类列表查询（前台/后台）、分类增删改
 *
 * @author cc
 * @date 2026-09-06
 */
@Slf4j
@Service
public class KnowledgeCategoryService {

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    /**
     * 查询启用状态的分类列表（前台，按排序字段升序）
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
                        .icon(category.getIcon())
                        .sortOrder(category.getSortOrder())
                        .build())
                .toList();
    }

    /**
     * 查询全部分类（含文章数量统计，后台分类管理使用，按 sortOrder asc 排序）
     *
     * @return 分类列表（含 articleCount）
     */
    public List<CategoryWithCountResponseDTO> listAllWithCount() {
        return knowledgeCategoryMapper.selectAllWithCount();
    }

    /**
     * 新增分类
     *
     * @param saveDTO 分类信息
     * @return 分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(CategorySaveDTO saveDTO) {
        KnowledgeCategory category = new KnowledgeCategory();
        category.setParentId(saveDTO.getParentId() != null ? saveDTO.getParentId() : 0L);
        category.setCategoryName(saveDTO.getCategoryName());
        category.setCategoryCode(saveDTO.getCategoryCode());
        category.setDescription(saveDTO.getDescription());
        category.setIcon(saveDTO.getIcon());
        category.setSortOrder(saveDTO.getSortOrder() != null ? saveDTO.getSortOrder() : 0);
        category.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 1);
        knowledgeCategoryMapper.insert(category);

        log.info("分类已创建, id={}, name={}", category.getId(), category.getCategoryName());
        return category.getId();
    }

    /**
     * 更新分类
     *
     * @param id      分类ID
     * @param saveDTO 分类信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategorySaveDTO saveDTO) {
        KnowledgeCategory existing = knowledgeCategoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }

        existing.setParentId(saveDTO.getParentId() != null ? saveDTO.getParentId() : existing.getParentId());
        existing.setCategoryName(saveDTO.getCategoryName());
        existing.setCategoryCode(saveDTO.getCategoryCode());
        existing.setDescription(saveDTO.getDescription());
        existing.setIcon(saveDTO.getIcon());
        existing.setSortOrder(saveDTO.getSortOrder() != null ? saveDTO.getSortOrder() : existing.getSortOrder());
        if (saveDTO.getStatus() != null) {
            existing.setStatus(saveDTO.getStatus());
        }
        knowledgeCategoryMapper.updateById(existing);

        log.info("分类已更新, id={}", id);
    }

    /**
     * 删除分类（FK RESTRICT：有文章的分类不可删除）
     *
     * @param id 分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (knowledgeCategoryMapper.selectById(id) == null) {
            throw new BusinessException("分类不存在");
        }
        knowledgeCategoryMapper.deleteById(id);
        log.info("分类已删除, id={}", id);
    }
}
