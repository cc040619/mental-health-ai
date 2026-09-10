package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.BrowseHistoryResponseDTO;
import com.cc.entity.UserBrowseHistory;
import com.cc.exception.BusinessException;
import com.cc.mapper.KnowledgeArticleMapper;
import com.cc.mapper.UserBrowseHistoryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 浏览历史业务层，处理文章浏览记录的写入、分页查询与清理
 *
 * <p>同一用户重复浏览同一文章时通过唯一索引兜底，仅更新最近浏览时间，不新增重复记录。</p>
 *
 * @author cc
 * @date 2026-09-07
 */
@Slf4j
@Service
public class BrowseHistoryService {

    /** 分页大小上限，防止超大 pageSize */
    private static final int MAX_PAGE_SIZE = 50;

    @Resource
    private UserBrowseHistoryMapper userBrowseHistoryMapper;

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;

    /**
     * 记录浏览历史（重复浏览更新最近浏览时间）
     *
     * @param userId    当前登录用户ID
     * @param articleId 文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordHistory(Long userId, String articleId) {
        if (!StringUtils.hasText(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }
        // 校验文章是否存在
        if (knowledgeArticleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在");
        }

        // 查询是否已有历史记录（重复浏览只更新时间）
        LambdaQueryWrapper<UserBrowseHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBrowseHistory::getUserId, userId)
                .eq(UserBrowseHistory::getArticleId, articleId);
        UserBrowseHistory existing = userBrowseHistoryMapper.selectOne(queryWrapper);

        if (existing != null) {
            existing.setCreatedAt(LocalDateTime.now());
            userBrowseHistoryMapper.updateById(existing);
        } else {
            UserBrowseHistory history = new UserBrowseHistory();
            history.setUserId(userId);
            history.setArticleId(articleId);
            history.setCreatedAt(LocalDateTime.now());
            userBrowseHistoryMapper.insert(history);
        }
    }

    /**
     * 分页查询当前用户浏览历史（按最近浏览时间倒序）
     *
     * @param userId 当前登录用户ID
     * @param page   页码
     * @param size   每页条数（上限 50）
     * @return 分页结果
     */
    public IPage<BrowseHistoryResponseDTO> getHistoryPage(Long userId, Long page, Long size) {
        long currentPage = page != null && page > 0 ? page : 1L;
        long pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : 10L;

        Page<BrowseHistoryResponseDTO> pageObj = new Page<>(currentPage, pageSize);
        return userBrowseHistoryMapper.selectHistoryPage(pageObj, userId);
    }

    /**
     * 删除单条浏览历史（校验归属，防止越权删除他人记录）
     *
     * @param userId    当前登录用户ID
     * @param historyId 历史记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteHistory(Long userId, Long historyId) {
        LambdaQueryWrapper<UserBrowseHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBrowseHistory::getId, historyId)
                .eq(UserBrowseHistory::getUserId, userId);
        int deleted = userBrowseHistoryMapper.delete(queryWrapper);
        if (deleted == 0) {
            throw new BusinessException("浏览历史记录不存在");
        }
    }

    /**
     * 清空当前用户全部浏览历史
     *
     * @param userId 当前登录用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long userId) {
        LambdaQueryWrapper<UserBrowseHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBrowseHistory::getUserId, userId);
        userBrowseHistoryMapper.delete(queryWrapper);
        log.info("用户清空浏览历史, userId={}", userId);
    }
}
