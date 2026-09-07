package com.cc.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ArticleCreateDTO;
import com.cc.dto.request.ArticlePageQueryDTO;
import com.cc.dto.response.ArticleResponseDTO;
import com.cc.dto.response.ArticleSimpleResponseDTO;
import com.cc.entity.KnowledgeArticle;
import com.cc.entity.KnowledgeCategory;
import com.cc.exception.BusinessException;
import com.cc.mapper.KnowledgeArticleMapper;
import com.cc.mapper.KnowledgeCategoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 知识文章业务层，处理文章分页查询、详情、创建、更新、状态变更与删除
 *
 * @author cc
 * @date 2026-09-06
 */
@Service
public class KnowledgeArticleService {

    /** 状态取值：0草稿 1已发布 2已下线 */
    private static final Set<Integer> VALID_STATUS = Set.of(0, 1, 2);

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    /**
     * 分页查询文章列表
     *
     * <p>带 sortField（前端文章列表）时强制仅查询已发布文章并按指定字段排序；
     * 管理端按更新时间倒序，支持标题/分类/状态筛选。</p>
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    public IPage<ArticleSimpleResponseDTO> page(ArticlePageQueryDTO queryDTO) {
        String sortColumn;
        String sortDirection;
        Integer status = queryDTO.getStatus();

        if (StringUtils.hasText(queryDTO.getSortField())) {
            // 前端文章列表：仅展示已发布文章
            status = 1;
            sortColumn = "readCount".equals(queryDTO.getSortField()) ? "a.read_count" : "a.published_at";
            sortDirection = "asc".equalsIgnoreCase(queryDTO.getSortDirection()) ? "ASC" : "DESC";
        } else {
            // 管理端：按更新时间倒序
            sortColumn = "a.updated_at";
            sortDirection = "DESC";
        }

        Page<ArticleSimpleResponseDTO> page = new Page<>(
                queryDTO.getCurrentPage() != null && queryDTO.getCurrentPage() > 0 ? queryDTO.getCurrentPage() : 1,
                queryDTO.getSize() != null && queryDTO.getSize() > 0 ? queryDTO.getSize() : 10);
        return knowledgeArticleMapper.selectPageWithRelation(page, queryDTO, sortColumn, sortDirection);
    }

    /**
     * 查询文章详情，阅读次数自增
     *
     * @param id 文章ID
     * @return 文章详情
     */
    public ArticleResponseDTO detail(String id) {
        // 阅读次数自增
        knowledgeArticleMapper.update(null,
                new LambdaUpdateWrapper<KnowledgeArticle>()
                        .eq(KnowledgeArticle::getId, id)
                        .setSql("read_count = read_count + 1"));

        ArticleResponseDTO article = knowledgeArticleMapper.selectDetailById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setTagArray(splitTags(article.getTags()));
        return article;
    }

    /**
     * 创建文章（使用前端生成的 UUID 作为主键），默认保存为草稿
     *
     * @param authorId 作者（当前登录用户）ID
     * @param createDTO 文章内容
     * @return 文章ID
     */
    public String create(Long authorId, ArticleCreateDTO createDTO) {
        String id = StringUtils.hasText(createDTO.getId()) ? createDTO.getId() : java.util.UUID.randomUUID().toString();

        if (knowledgeArticleMapper.selectById(id) != null) {
            throw new BusinessException("文章已存在");
        }
        checkCategoryExists(createDTO.getCategoryId());

        KnowledgeArticle article = KnowledgeArticle.builder()
                .id(id)
                .categoryId(createDTO.getCategoryId())
                .title(createDTO.getTitle())
                .summary(createDTO.getSummary())
                .content(createDTO.getContent())
                .coverImage(createDTO.getCoverImage())
                .tags(createDTO.getTags())
                .authorId(authorId)
                .readCount(0)
                .status(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        knowledgeArticleMapper.insert(article);
        return id;
    }

    /**
     * 更新文章内容
     *
     * @param id        文章ID
     * @param createDTO 文章内容
     */
    public void update(String id, ArticleCreateDTO createDTO) {
        KnowledgeArticle existing = knowledgeArticleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }
        checkCategoryExists(createDTO.getCategoryId());

        existing.setCategoryId(createDTO.getCategoryId());
        existing.setTitle(createDTO.getTitle());
        existing.setSummary(createDTO.getSummary());
        existing.setContent(createDTO.getContent());
        existing.setCoverImage(createDTO.getCoverImage());
        existing.setTags(createDTO.getTags());
        existing.setUpdatedAt(LocalDateTime.now());
        knowledgeArticleMapper.updateById(existing);
    }

    /**
     * 更新文章状态（发布时记录发布时间）
     *
     * @param id     文章ID
     * @param status 目标状态 0草稿 1发布 2下线
     */
    public void updateStatus(String id, Integer status) {
        if (!VALID_STATUS.contains(status)) {
            throw new BusinessException("无效的文章状态");
        }
        KnowledgeArticle existing = knowledgeArticleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }

        existing.setStatus(status);
        if (status == 1) {
            existing.setPublishedAt(LocalDateTime.now());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        knowledgeArticleMapper.updateById(existing);
    }

    /**
     * 删除文章
     *
     * @param id 文章ID
     */
    public void delete(String id) {
        if (knowledgeArticleMapper.selectById(id) == null) {
            throw new BusinessException("文章不存在");
        }
        knowledgeArticleMapper.deleteById(id);
    }

    /**
     * 校验分类是否存在
     */
    private void checkCategoryExists(Long categoryId) {
        if (categoryId == null || knowledgeCategoryMapper.selectById(categoryId) == null) {
            throw new BusinessException("文章分类不存在");
        }
    }

    /**
     * 将逗号分隔的标签字符串拆分为数组
     */
    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }
}
