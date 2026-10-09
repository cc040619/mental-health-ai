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
import com.cc.dto.response.ArticleStatsResponseDTO;
import com.cc.dto.response.NameCountDTO;
import com.cc.entity.ArticleLike;
import com.cc.entity.KnowledgeArticle;
import com.cc.entity.KnowledgeCategory;
import com.cc.entity.UserBrowseHistory;
import com.cc.exception.BusinessException;
import com.cc.mapper.ArticleLikeMapper;
import com.cc.mapper.KnowledgeArticleMapper;
import com.cc.mapper.KnowledgeCategoryMapper;
import com.cc.mapper.UserBrowseHistoryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识文章业务层，处理文章分页查询、详情、创建、更新、状态变更、删除、
 * 批量操作、点赞 toggle、阅读时长上报与文章数据统计
 *
 * @author cc
 * @date 2026-09-06
 */
@Slf4j
@Service
public class KnowledgeArticleService {

    /** 状态取值：0草稿 1已发布 2已下线 */
    private static final Set<Integer> VALID_STATUS = Set.of(0, 1, 2);

    /** 已发布状态：RAG 建索引与检索唯一可见口径（管理员同样只看已发布） */
    private static final int PUBLISHED_STATUS = 1;

    /** 批量上架/下架允许的目标状态 */
    private static final Set<Integer> BATCH_STATUS_ALLOWED = Set.of(1, 2);

    /** 管理端排序字段白名单（防 SQL 注入，不接受任意列名） */
    private static final Map<String, String> SORT_COLUMN_MAP = Map.of(
            "readCount", "a.read_count",
            "likeCount", "a.like_count",
            "commentCount", "a.comment_count",
            "favoriteCount", "a.favorite_count",
            "createdAt", "a.created_at"
    );

    /** 计数字段白名单（incrementCount 安全约束） */
    private static final Set<String> COUNT_COLUMNS = Set.of("like_count", "comment_count", "favorite_count");

    /** 阅读时长上报最小间隔（秒），30 秒内重复上报忽略 */
    private static final long READ_DURATION_MIN_INTERVAL = 30L;

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Resource
    private ArticleLikeMapper articleLikeMapper;

    @Resource
    private UserBrowseHistoryMapper userBrowseHistoryMapper;

    /**
     * 分页查询文章列表
     *
     * <p>带 sortField（前端文章列表）时强制仅查询已发布文章并按指定字段排序；
     * 管理端支持 sortBy（readCount/likeCount/commentCount/favoriteCount/createdAt）排序，默认按更新时间倒序。</p>
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    public IPage<ArticleSimpleResponseDTO> page(ArticlePageQueryDTO queryDTO) {
        String sortColumn;
        String sortDirection;

        if (StringUtils.hasText(queryDTO.getSortField())) {
            // 前端文章列表：仅展示已发布文章
            queryDTO.setStatus(1);
            sortColumn = "readCount".equals(queryDTO.getSortField()) ? "a.read_count" : "a.published_at";
            sortDirection = "asc".equalsIgnoreCase(queryDTO.getSortDirection()) ? "ASC" : "DESC";
        } else if (StringUtils.hasText(queryDTO.getSortBy())) {
            // 管理端：白名单校验排序字段
            sortColumn = SORT_COLUMN_MAP.get(queryDTO.getSortBy());
            if (sortColumn == null) {
                throw new BusinessException("无效的排序字段");
            }
            sortDirection = "asc".equalsIgnoreCase(queryDTO.getOrder()) ? "ASC" : "DESC";
        } else {
            // 管理端默认：按更新时间倒序
            sortColumn = "a.updated_at";
            sortDirection = "DESC";
        }

        long pageSize = queryDTO.getSize() != null && queryDTO.getSize() > 0 ? Math.min(queryDTO.getSize(), 20L) : 10L;
        long currentPage = queryDTO.getCurrentPage() != null && queryDTO.getCurrentPage() > 0 ? queryDTO.getCurrentPage() : 1L;
        Page<ArticleSimpleResponseDTO> page = new Page<>(currentPage, pageSize);
        return knowledgeArticleMapper.selectPageWithRelation(page, queryDTO, sortColumn, sortDirection);
    }

    // ==================== RAG 索引/检索只读查询 ====================

    /**
     * 全量已发布文章（RAG 建索引用），仅标题/摘要/正文参与检索，不含草稿与已下线
     *
     * @param limit 上限条数
     */
    public List<KnowledgeArticle> listAllPublished(int limit) {
        return knowledgeArticleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, PUBLISHED_STATUS)
                .orderByDesc(KnowledgeArticle::getPublishedAt)
                .last("LIMIT " + Math.max(1, limit)));
    }

    /**
     * 按 ID 批量回查已发布文章：向量命中后复核可见性（索引可能落后于库）
     *
     * @param ids 文章ID列表
     */
    public List<KnowledgeArticle> listPublishedByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return knowledgeArticleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, PUBLISHED_STATUS)
                .in(KnowledgeArticle::getId, ids));
    }

    /**
     * 关键词检索已发布文章（标题或摘要 LIKE），供 RAG 关键词降级路径使用
     *
     * @param keyword 关键词（调用方已清洗，≤50 字）
     * @param limit   上限条数
     */
    public List<KnowledgeArticle> searchPublishedByKeyword(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return knowledgeArticleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, PUBLISHED_STATUS)
                .and(w -> w.like(KnowledgeArticle::getTitle, keyword)
                        .or().like(KnowledgeArticle::getSummary, keyword))
                .orderByDesc(KnowledgeArticle::getPublishedAt)
                .last("LIMIT " + Math.max(1, limit)));
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
                .likeCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .status(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        knowledgeArticleMapper.insert(article);
        return id;
    }

    /**
     * 更新文章内容
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
     * 删除文章（物理删除，外键 CASCADE 自动清理收藏/浏览/点赞/评论）
     */
    public void delete(String id) {
        if (knowledgeArticleMapper.selectById(id) == null) {
            throw new BusinessException("文章不存在");
        }
        knowledgeArticleMapper.deleteById(id);
    }

    // ==================== 批量操作 ====================

    /**
     * 批量上架/下架（单条 UPDATE ... IN，事务）
     *
     * @param ids    文章ID列表（≤100）
     * @param status 目标状态 1:已发布 2:已下线
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<String> ids, Integer status) {
        if (!BATCH_STATUS_ALLOWED.contains(status)) {
            throw new BusinessException("批量操作仅支持上架(1)或下架(2)");
        }
        knowledgeArticleMapper.batchUpdateStatus(ids, status);
        log.info("批量更新文章状态, count={}, status={}", ids.size(), status);
    }

    /**
     * 批量删除（物理删除，外键 CASCADE 自动清理收藏/浏览/点赞/评论）
     *
     * @param ids 文章ID列表（≤100）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<String> ids) {
        knowledgeArticleMapper.deleteBatchIds(ids);
        log.info("批量删除文章, count={}", ids.size());
    }

    /**
     * 批量移动分类（单条 UPDATE ... IN，事务，校验分类存在）
     *
     * @param ids        文章ID列表（≤100）
     * @param categoryId 目标分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateCategory(List<String> ids, Long categoryId) {
        checkCategoryExists(categoryId);
        knowledgeArticleMapper.batchUpdateCategory(ids, categoryId);
        log.info("批量移动文章分类, count={}, categoryId={}", ids.size(), categoryId);
    }

    // ==================== 点赞 toggle ====================

    /**
     * 点赞 toggle（前台，article_like 唯一索引兜底 + like_count 同步）
     *
     * @param userId    当前登录用户ID
     * @param articleId 文章ID
     * @return true=已点赞 false=已取消
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long userId, String articleId) {
        if (knowledgeArticleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在");
        }

        LambdaQueryWrapper<ArticleLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId);
        ArticleLike existing = articleLikeMapper.selectOne(queryWrapper);

        if (existing != null) {
            // 已点赞 → 取消
            articleLikeMapper.deleteById(existing.getId());
            incrementCountSafe(articleId, "like_count", -1);
            return false;
        } else {
            // 未点赞 → 点赞（唯一索引兜底防并发重复）
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            articleLikeMapper.insert(like);
            incrementCountSafe(articleId, "like_count", 1);
            return true;
        }
    }

    /**
     * 判断当前用户是否已点赞
     */
    public boolean isLiked(Long userId, String articleId) {
        if (userId == null || !StringUtils.hasText(articleId)) {
            return false;
        }
        LambdaQueryWrapper<ArticleLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId);
        return articleLikeMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 查询文章各项计数（点赞/评论/收藏/阅读），不自增阅读次数，前台 toggle 后回显用
     *
     * @param articleId 文章ID
     * @return {likeCount, commentCount, favoriteCount, readCount}
     */
    public Map<String, Integer> getCounts(String articleId) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        Map<String, Integer> counts = new java.util.HashMap<>();
        counts.put("likeCount", article.getLikeCount());
        counts.put("commentCount", article.getCommentCount());
        counts.put("favoriteCount", article.getFavoriteCount());
        counts.put("readCount", article.getReadCount());
        return counts;
    }

    // ==================== 阅读时长上报 ====================

    /**
     * 上报阅读时长（累加到 user_browse_history.duration_seconds，30 秒内重复忽略）
     *
     * @param userId    当前登录用户ID
     * @param articleId 文章ID
     * @param seconds   本次阅读时长（1..7200 秒）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportReadDuration(Long userId, String articleId, int seconds) {
        if (knowledgeArticleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在");
        }

        LambdaQueryWrapper<UserBrowseHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBrowseHistory::getUserId, userId)
                .eq(UserBrowseHistory::getArticleId, articleId);
        UserBrowseHistory existing = userBrowseHistoryMapper.selectOne(queryWrapper);

        if (existing == null) {
            // 无浏览记录时新建（极少见：详情页加载时会调 recordHistory）
            UserBrowseHistory history = new UserBrowseHistory();
            history.setUserId(userId);
            history.setArticleId(articleId);
            history.setDurationSeconds(seconds);
            history.setCreatedAt(LocalDateTime.now());
            userBrowseHistoryMapper.insert(history);
            return;
        }

        // 防刷：距上次更新（created_at）不足 30 秒则忽略
        long interval = Duration.between(existing.getCreatedAt(), LocalDateTime.now()).getSeconds();
        if (interval < READ_DURATION_MIN_INTERVAL) {
            log.debug("阅读时长上报被限频忽略, userId={}, articleId={}, interval={}s", userId, articleId, interval);
            return;
        }

        existing.setDurationSeconds((existing.getDurationSeconds() == null ? 0 : existing.getDurationSeconds()) + seconds);
        existing.setCreatedAt(LocalDateTime.now());
        userBrowseHistoryMapper.updateById(existing);
    }

    // ==================== 文章数据统计 ====================

    /**
     * 查询文章数据统计（近30天浏览趋势、阅读时长、各项计数）
     *
     * @param articleId 文章ID
     * @return 统计数据
     */
    public ArticleStatsResponseDTO getStats(String articleId) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        ArticleStatsResponseDTO stats = new ArticleStatsResponseDTO();
        stats.setReadCount(article.getReadCount());
        stats.setLikeCount(article.getLikeCount());
        stats.setCommentCount(article.getCommentCount());
        stats.setFavoriteCount(article.getFavoriteCount());

        // 近30天浏览趋势
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<NameCountDTO> dailyBrowses = userBrowseHistoryMapper.selectDailyBrowseTrend(articleId, startDate);
        stats.setDailyBrowses(fillMissingDates(dailyBrowses, startDate));

        // 阅读时长统计
        Map<String, Object> durationStats = userBrowseHistoryMapper.selectDurationStats(articleId);
        stats.setAvgDuration(toLong(durationStats.get("avgDuration")));
        stats.setTotalDuration(toLong(durationStats.get("totalDuration")));
        stats.setTotalViews(toLong(durationStats.get("totalViews")));

        return stats;
    }

    // ==================== 内部工具 ====================

    /**
     * 计数自增（白名单校验后调用 incrementCount）
     */
    private void incrementCountSafe(String articleId, String column, int delta) {
        if (!COUNT_COLUMNS.contains(column)) {
            throw new BusinessException("无效的计数字段: " + column);
        }
        knowledgeArticleMapper.incrementCount(articleId, column, delta);
    }

    /**
     * 补零：将查询出的每日浏览数据按日期补齐缺失天为 0
     */
    private List<NameCountDTO> fillMissingDates(List<NameCountDTO> dailyBrowses, LocalDateTime startDate) {
        // ponytail: 数据量小（30天），直接用 LinkedHashMap 补零
        java.util.Map<String, Long> dateMap = new java.util.LinkedHashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            String dateStr = today.minusDays(i).toString();
            dateMap.put(dateStr, 0L);
        }
        for (NameCountDTO item : dailyBrowses) {
            dateMap.put(item.getName(), item.getValue());
        }
        return dateMap.entrySet().stream()
                .map(e -> new NameCountDTO(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * Map 取值转 Long（兼容 Number 与 null）
     */
    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0L;
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
