package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.FavoriteResponseDTO;
import com.cc.entity.UserFavorite;
import com.cc.exception.BusinessException;
import com.cc.mapper.KnowledgeArticleMapper;
import com.cc.mapper.UserFavoriteMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户收藏业务层，处理文章收藏、取消收藏与分页查询
 *
 * @author cc
 * @date 2026-09-07
 */
@Slf4j
@Service
public class FavoriteService {

    /** 分页大小上限，防止超大 pageSize */
    private static final int MAX_PAGE_SIZE = 50;

    @Resource
    private UserFavoriteMapper userFavoriteMapper;

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;

    /**
     * 收藏文章（唯一索引兜底 + 业务判重，同步 favorite_count +1）
     *
     * @param userId    当前登录用户ID
     * @param articleId 文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, String articleId) {
        if (!StringUtils.hasText(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }
        // 校验文章是否存在
        if (knowledgeArticleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在");
        }
        // 业务判重：已收藏则直接返回，唯一索引兜底防止并发重复
        LambdaQueryWrapper<UserFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getArticleId, articleId);
        if (userFavoriteMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("您已收藏过该文章");
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setArticleId(articleId);
        favorite.setCreatedAt(LocalDateTime.now());
        userFavoriteMapper.insert(favorite);

        // 同步冗余计数 +1
        knowledgeArticleMapper.incrementCount(articleId, "favorite_count", 1);

        log.info("用户收藏文章成功, userId={}, articleId={}", userId, articleId);
    }

    /**
     * 分页查询当前用户收藏列表
     *
     * @param userId 当前登录用户ID
     * @param page   页码
     * @param size   每页条数（上限 50）
     * @return 分页结果
     */
    public IPage<FavoriteResponseDTO> getFavoritePage(Long userId, Long page, Long size) {
        long currentPage = page != null && page > 0 ? page : 1L;
        long pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : 10L;

        Page<FavoriteResponseDTO> pageObj = new Page<>(currentPage, pageSize);
        return userFavoriteMapper.selectFavoritePage(pageObj, userId);
    }

    /**
     * 取消收藏（同步 favorite_count -1）
     *
     * @param userId    当前登录用户ID
     * @param articleId 文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, String articleId) {
        if (!StringUtils.hasText(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }
        LambdaQueryWrapper<UserFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getArticleId, articleId);
        int deleted = userFavoriteMapper.delete(queryWrapper);
        if (deleted == 0) {
            throw new BusinessException("未找到对应的收藏记录");
        }

        // 同步冗余计数 -1
        knowledgeArticleMapper.incrementCount(articleId, "favorite_count", -1);

        log.info("用户取消收藏成功, userId={}, articleId={}", userId, articleId);
    }

    /**
     * 判断当前用户是否已收藏某篇文章（文章详情页使用）
     *
     * @param userId    当前登录用户ID
     * @param articleId 文章ID
     * @return true=已收藏
     */
    public boolean isFavorited(Long userId, String articleId) {
        if (!StringUtils.hasText(articleId)) {
            return false;
        }
        LambdaQueryWrapper<UserFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getArticleId, articleId);
        return userFavoriteMapper.selectCount(queryWrapper) > 0;
    }
}
