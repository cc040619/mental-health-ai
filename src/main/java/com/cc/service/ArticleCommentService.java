package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.ArticleCommentResponseDTO;
import com.cc.entity.ArticleComment;
import com.cc.exception.BusinessException;
import com.cc.mapper.ArticleCommentMapper;
import com.cc.mapper.KnowledgeArticleMapper;
import com.cc.util.SensitiveWordFilter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 文章评论业务层
 *
 * <p>评论发表 → 过敏感词过滤 → 默认 status=0 待审核 → 管理员审核通过(1)/拒绝(2)。
 * knowledge_article.comment_count 仅统计 status=1 的评论，审核通过时 +1，
 * 删除已通过评论时 -1，拒绝不增加。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class ArticleCommentService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private static final int MAX_FRONT_PAGE_SIZE = 50;
    private static final int MAX_ADMIN_PAGE_SIZE = 20;

    @Resource
    private ArticleCommentMapper articleCommentMapper;

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    /**
     * 发表评论（前台，过敏感词过滤，默认待审核）
     *
     * @param userId    当前登录用户ID
     * @param articleId 文章ID
     * @param content   评论内容（≤500字）
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Long userId, String articleId, String content) {
        if (knowledgeArticleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在");
        }
        // 敏感词过滤：命中直接拒绝，不存储
        sensitiveWordFilter.checkSensitive(content);

        ArticleComment comment = new ArticleComment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setStatus(STATUS_PENDING);
        comment.setCreatedAt(LocalDateTime.now());
        articleCommentMapper.insert(comment);

        log.info("评论已提交待审核, userId={}, articleId={}", userId, articleId);
    }

    /**
     * 前台查询某文章已通过评论列表（status=1，分页）
     *
     * @param articleId 文章ID
     * @param page      页码
     * @param size      每页条数（上限 50）
     * @return 分页结果
     */
    public IPage<ArticleCommentResponseDTO> listApproved(String articleId, Long page, Long size) {
        long currentPage = page != null && page > 0 ? page : 1L;
        long pageSize = size != null && size > 0 ? Math.min(size, MAX_FRONT_PAGE_SIZE) : 10L;
        Page<ArticleCommentResponseDTO> pageObj = new Page<>(currentPage, pageSize);
        return articleCommentMapper.selectApprovedPage(pageObj, articleId);
    }

    /**
     * 后台分页查询评论（支持状态/文章筛选）
     *
     * @param status    评论状态 0/1/2，null 表示全部
     * @param articleId 文章ID，null 表示全部
     * @param page      页码
     * @param size      每页条数（上限 20）
     * @return 分页结果
     */
    public IPage<ArticleCommentResponseDTO> adminPage(Integer status, String articleId, Long page, Long size) {
        long currentPage = page != null && page > 0 ? page : 1L;
        long pageSize = size != null && size > 0 ? Math.min(size, MAX_ADMIN_PAGE_SIZE) : 10L;
        Page<ArticleCommentResponseDTO> pageObj = new Page<>(currentPage, pageSize);
        return articleCommentMapper.selectCommentPage(pageObj, status, articleId);
    }

    /**
     * 审核评论（仅 status=0 可审；通过→1+comment_count+1，拒绝→2）
     *
     * @param commentId 评论ID
     * @param approve   true=通过 false=拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public void review(Long commentId, boolean approve) {
        ArticleComment comment = articleCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (comment.getStatus() != STATUS_PENDING) {
            throw new BusinessException("该评论已审核，不可重复审核");
        }

        comment.setStatus(approve ? STATUS_APPROVED : STATUS_REJECTED);
        articleCommentMapper.updateById(comment);

        // 仅审核通过时 comment_count +1（拒绝不增加，保证 = 前台可见数）
        if (approve) {
            knowledgeArticleMapper.incrementCount(comment.getArticleId(), "comment_count", 1);
        }

        log.info("评论审核完成, commentId={}, approve={}", commentId, approve);
    }

    /**
     * 删除评论（物理删；若原 status=1 需 comment_count-1）
     *
     * @param commentId 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long commentId) {
        ArticleComment comment = articleCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        // 已通过评论被删除时，comment_count -1 保持口径一致
        if (comment.getStatus() == STATUS_APPROVED) {
            knowledgeArticleMapper.incrementCount(comment.getArticleId(), "comment_count", -1);
        }
        articleCommentMapper.deleteById(commentId);
        log.info("评论已删除, commentId={}", commentId);
    }

    /**
     * 查询某文章已通过评论数（前台展示用）
     */
    public long countApproved(String articleId) {
        LambdaQueryWrapper<ArticleComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleComment::getArticleId, articleId)
                .eq(ArticleComment::getStatus, STATUS_APPROVED);
        return articleCommentMapper.selectCount(queryWrapper);
    }
}
