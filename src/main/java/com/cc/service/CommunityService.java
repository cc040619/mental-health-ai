package com.cc.service;

import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.common.RedisConstants;
import com.cc.dto.request.CommentCreateDTO;
import com.cc.dto.request.PostCreateDTO;
import com.cc.dto.request.ReportCreateDTO;
import com.cc.dto.response.CommentResponseDTO;
import com.cc.dto.response.CommunityMineResponseDTO;
import com.cc.dto.response.PostResponseDTO;
import com.cc.dto.response.ReportResponseDTO;
import com.cc.entity.CommunityComment;
import com.cc.entity.CommunityFavorite;
import com.cc.entity.CommunityLike;
import com.cc.entity.CommunityPost;
import com.cc.entity.CommunityReport;
import com.cc.entity.User;
import com.cc.enums.CommunityBoard;
import com.cc.enums.CommunityPostStatus;
import com.cc.enums.CommunityReportStatus;
import com.cc.enums.CommunityReportTarget;
import com.cc.exception.BusinessException;
import com.cc.mapper.CommunityCommentMapper;
import com.cc.mapper.CommunityFavoriteMapper;
import com.cc.mapper.CommunityLikeMapper;
import com.cc.mapper.CommunityPostMapper;
import com.cc.mapper.CommunityReportMapper;
import com.cc.mapper.UserMapper;
import com.cc.util.CurrentUserUtils;
import com.cc.util.HtmlSanitizer;
import com.cc.util.SensitiveWordFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 社区论坛业务层
 *
 * <p>缓存策略（Cache-Aside + 延时双删一致性）：</p>
 * <ul>
 *   <li>热门榜 community:hot（TTL 5min）、帖子详情 community:post:{id}（TTL 10min）、板块列表（TTL 1d）走 Redis</li>
 *   <li>评论列表、搜索、我的帖子不缓存，直接查库</li>
 *   <li>写路径采用延时双删：先删缓存 → 更新 MySQL → 随机延时（500~1500ms）→ 再删缓存，
 *       兜底并发读写期间 binlog 复制/读写时差导致的旧值回填；定时任务每 5 分钟重建热门榜兜底</li>
 *   <li>浏览数 +1 高频写不走双删，接受 TTL 内最多 10 分钟 viewCount 延迟，换取详情缓存命中率</li>
 *   <li>Redis 异常 fail-open 回退 DB，不阻塞主流程</li>
 * </ul>
 *
 * <p>安全：发帖/评论强制敏感词 DFA 校验；富文本存库前白名单清洗；危机词标记不拦截并返回干预提示；
 * 匿名帖子对外统一「匿名用户」，仅作者本人/管理员可见真实身份；删帖/删评论校验本人或管理员。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@Slf4j
@Service
public class CommunityService {

    /** 热门榜缓存条数 */
    private static final int HOT_LIST_SIZE = 50;

    /** 我的帖子/收藏返回上限 */
    private static final int MINE_LIMIT = 50;

    @Resource
    private CommunityPostMapper postMapper;
    @Resource
    private CommunityCommentMapper commentMapper;
    @Resource
    private CommunityLikeMapper likeMapper;
    @Resource
    private CommunityFavoriteMapper favoriteMapper;
    @Resource
    private CommunityReportMapper reportMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private SensitiveWordFilter sensitiveWordFilter;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    /** 延时双删执行器（单线程守护，仅做轻量 key 删除，应用关闭即回收） */
    // ponytail: 单线程足够覆盖社区写量；写量成瓶颈时改用 Spring TaskScheduler 共享池
    private final ScheduledExecutorService doubleDeleteExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "community-cache-double-delete");
                t.setDaemon(true);
                return t;
            });

    /** 延时双删随机延时下限（毫秒） */
    private static final long DOUBLE_DELETE_DELAY_MIN_MS = 500L;
    /** 延时双删随机延时上限（毫秒） */
    private static final long DOUBLE_DELETE_DELAY_MAX_MS = 1500L;

    @PreDestroy
    public void shutdown() {
        doubleDeleteExecutor.shutdown();
    }

    /**
     * 延时双删：立即删帖子详情 + 热门榜缓存，随机延时后再删一次。
     * <p>随机延时避开并发读写场景下「先删缓存→读旧值入缓存→更新DB→缓存仍是旧值」的固定窗口。
     * 第二删在事务提交后执行（500~1500ms 足以覆盖常规事务提交时延）。</p>
     */
    private void evictPostCacheDoubleDelete(Long postId) {
        evictPostCacheNow(postId);
        long delay = DOUBLE_DELETE_DELAY_MIN_MS
                + (long) (Math.random() * (DOUBLE_DELETE_DELAY_MAX_MS - DOUBLE_DELETE_DELAY_MIN_MS));
        doubleDeleteExecutor.schedule(() -> evictPostCacheNow(postId), delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 立即删除帖子详情缓存 + 热门榜缓存（fail-open，Redis 异常仅告警不抛）
     */
    private void evictPostCacheNow(Long postId) {
        try {
            stringRedisTemplate.delete(RedisConstants.buildCommunityPostCacheKey(postId));
            stringRedisTemplate.delete(RedisConstants.COMMUNITY_HOT_KEY);
        } catch (Exception e) {
            log.warn("延时双删删除缓存失败（fail-open）: postId={}, err={}", postId, e.getMessage());
        }
    }

    /**
     * 仅删热门榜缓存（新帖入库场景：帖子本身无详情缓存，但热门榜需失效）
     */
    private void evictHotCacheDoubleDelete() {
        try {
            stringRedisTemplate.delete(RedisConstants.COMMUNITY_HOT_KEY);
        } catch (Exception e) {
            log.warn("删除热门榜缓存失败（fail-open）: {}", e.getMessage());
        }
        long delay = DOUBLE_DELETE_DELAY_MIN_MS
                + (long) (Math.random() * (DOUBLE_DELETE_DELAY_MAX_MS - DOUBLE_DELETE_DELAY_MIN_MS));
        doubleDeleteExecutor.schedule(() -> {
            try {
                stringRedisTemplate.delete(RedisConstants.COMMUNITY_HOT_KEY);
            } catch (Exception e) {
                log.warn("延时双删热门榜失败（fail-open）: {}", e.getMessage());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    // ==================== 信息流 ====================

    /**
     * 帖子信息流：latest/recommend 按时间倒序查库；hot 走 Redis 热门榜（miss 回源重建）
     */
    public Map<String, Object> listPosts(String board, String tab, Long page, Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean showReal = CurrentUserUtils.isAdmin();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 20L) : 10L;
        if (board != null && !board.isBlank() && CommunityBoard.fromCode(board) == null) {
            throw new BusinessException("板块不存在");
        }

        if ("hot".equalsIgnoreCase(tab)) {
            return hotPage(board, userId, cur, sz);
        }
        // recommend 预留：当前同 latest（创建时间倒序）
        Page<PostResponseDTO> p = new Page<>(cur, sz);
        IPage<PostResponseDTO> result = postMapper.selectPostPage(p, board, userId, showReal ? 1 : 0);
        List<PostResponseDTO> records = result.getRecords();
        polishList(records);
        return Map.of("records", records, "total", result.getTotal());
    }

    /**
     * 热门榜：读 Redis 缓存（全板块 50 条），内存按板块过滤 + 分页，再按当前用户补互动状态
     */
    private Map<String, Object> hotPage(String board, Long userId, long cur, long sz) {
        List<PostResponseDTO> hot = getHotList();
        if (board != null && !board.isBlank()) {
            hot = hot.stream().filter(p -> board.equals(p.getBoard())).collect(Collectors.toList());
        }
        long total = hot.size();
        int from = (int) Math.min((cur - 1) * sz, total);
        int to = (int) Math.min(from + sz, total);
        List<PostResponseDTO> pageRecords = new ArrayList<>(hot.subList(from, to));
        fillUserReactions(pageRecords, userId);
        return Map.of("records", pageRecords, "total", total);
    }

    /**
     * 获取热门榜（缓存 miss 回源 DB 并回填）
     */
    private List<PostResponseDTO> getHotList() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.COMMUNITY_HOT_KEY);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<List<PostResponseDTO>>() {
                });
            }
        } catch (Exception e) {
            log.warn("热门榜缓存读取异常，回源DB: {}", e.getMessage());
        }
        return rebuildHotCache();
    }

    /**
     * 重建热门榜缓存并返回列表（定时任务 / 缓存 miss / 冷启动共用）
     */
    public List<PostResponseDTO> rebuildHotCache() {
        List<PostResponseDTO> hot = postMapper.selectHotPosts(HOT_LIST_SIZE);
        polishList(hot);
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.COMMUNITY_HOT_KEY,
                    objectMapper.writeValueAsString(hot),
                    RedisConstants.COMMUNITY_HOT_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("热门榜缓存写入失败（fail-open）: {}", e.getMessage());
        }
        return hot;
    }

    // ==================== 帖子详情 ====================

    /**
     * 帖子详情：走 Redis 缓存（公共视角），命中后实时补当前用户互动状态；
     * 浏览数 +1 写库（高频写不走延时双删，接受 TTL 内最多 10 分钟 viewCount 延迟，换取详情缓存命中率）
     */
    public PostResponseDTO getPostDetail(Long postId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean isAdmin = CurrentUserUtils.isAdmin();

        PostResponseDTO dto = getDetailWithCache(postId);
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在或已删除");
        }
        boolean isMine = post.getAuthorId().equals(userId);
        // 非正常状态帖：仅作者本人或管理员可见
        if (post.getStatus() != null && post.getStatus() != CommunityPostStatus.NORMAL.getCode()
                && !isMine && !isAdmin) {
            throw new BusinessException("帖子不存在或已下架");
        }

        // 实时互动状态（不随详情缓存）
        dto.setIsMine(isMine);
        dto.setIsLiked(likeMapper.selectCount(new LambdaQueryWrapper<CommunityLike>()
                .eq(CommunityLike::getPostId, postId).eq(CommunityLike::getUserId, userId)) > 0);
        dto.setIsFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<CommunityFavorite>()
                .eq(CommunityFavorite::getPostId, postId).eq(CommunityFavorite::getUserId, userId)) > 0);

        // 匿名帖：作者本人/管理员可见真实昵称头像（缓存中为匿名屏蔽版）
        if (Integer.valueOf(1).equals(post.getIsAnonymous()) && (isMine || isAdmin)) {
            User author = userMapper.selectById(post.getAuthorId());
            if (author != null) {
                dto.setAuthorName(author.getDisplayName());
                dto.setAvatar(author.getAvatar());
            }
        }

        // 浏览数 +1（原子自增单行字段，不重算）
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .setSql("view_count = view_count + 1"));
        dto.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        return dto;
    }

    /**
     * 详情缓存读取（Cache-Aside），缓存为匿名屏蔽的公共视角
     */
    private PostResponseDTO getDetailWithCache(Long postId) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.buildCommunityPostCacheKey(postId));
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, PostResponseDTO.class);
            }
        } catch (Exception e) {
            log.warn("帖子详情缓存读取异常，回源DB: postId={}, err={}", postId, e.getMessage());
        }

        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在或已删除");
        }
        User author = userMapper.selectById(post.getAuthorId());

        PostResponseDTO dto = new PostResponseDTO();
        dto.setId(post.getId());
        dto.setBoard(post.getBoard());
        dto.setBoardLabel(boardLabel(post.getBoard()));
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setImages(post.getImages());
        dto.setAuthorId(post.getAuthorId());
        dto.setIsAnonymous(post.getIsAnonymous());
        // 匿名屏蔽：公共缓存统一「匿名用户」
        if (Integer.valueOf(1).equals(post.getIsAnonymous())) {
            dto.setAuthorName("匿名用户");
            dto.setAvatar(null);
        } else if (author != null) {
            dto.setAuthorName(author.getDisplayName());
            dto.setAvatar(author.getAvatar());
        }
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setFavoriteCount(post.getFavoriteCount());
        dto.setViewCount(post.getViewCount());
        dto.setIsCrisis(post.getIsCrisis());
        dto.setStatus(post.getStatus());
        dto.setCreatedAt(post.getCreatedAt());
        if (Integer.valueOf(1).equals(post.getIsCrisis())) {
            dto.setCrisisTip(buildCrisisTip());
        }

        // 仅缓存正常帖（异常态帖不回填，避免下架帖被缓存命中）
        if (post.getStatus() != null && post.getStatus() == CommunityPostStatus.NORMAL.getCode()) {
            try {
                stringRedisTemplate.opsForValue().set(
                        RedisConstants.buildCommunityPostCacheKey(postId),
                        objectMapper.writeValueAsString(dto),
                        RedisConstants.COMMUNITY_POST_CACHE_TTL_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (Exception e) {
                log.warn("帖子详情缓存写入失败（fail-open）: postId={}, err={}", postId, e.getMessage());
            }
        }
        return dto;
    }

    // ==================== 发帖 / 删帖 ====================

    /**
     * 发帖：板块校验 → HTML 白名单清洗 → 敏感词拦截 → 危机词标记 → 落库
     * <p>新帖无详情缓存，只需延时双删热门榜，下次读 hot 回填新帖</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public PostResponseDTO createPost(PostCreateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (CommunityBoard.fromCode(dto.getBoard()) == null) {
            throw new BusinessException("请选择正确的板块");
        }
        // 富文本 XSS 白名单清洗
        String cleanContent = HtmlSanitizer.sanitize(dto.getContent());
        // 敏感词强制校验（标题 + 正文），命中抛业务异常
        sensitiveWordFilter.checkSensitive(dto.getTitle() + cleanContent);
        // 危机词检测：不拦截，标记 is_crisis
        boolean crisis = sensitiveWordFilter.findCrisisWord(dto.getTitle() + cleanContent) != null;

        CommunityPost post = CommunityPost.builder()
                .board(dto.getBoard())
                .title(dto.getTitle().trim())
                .content(cleanContent)
                .images(dto.getImages())
                .authorId(userId)
                .isAnonymous(Integer.valueOf(1).equals(dto.getIsAnonymous()) ? 1 : 0)
                .status(CommunityPostStatus.NORMAL.getCode())
                .likeCount(0).commentCount(0).favoriteCount(0).viewCount(0)
                .isCrisis(crisis ? 1 : 0)
                .build();
        postMapper.insert(post);
        // 新帖入库触发热门榜延时双删（帖子本身无详情缓存可删）
        evictHotCacheDoubleDelete();
        log.info("社区发帖: userId={}, postId={}, board={}, crisis={}", userId, post.getId(), dto.getBoard(), crisis);

        PostResponseDTO resp = new PostResponseDTO();
        resp.setId(post.getId());
        resp.setBoard(post.getBoard());
        resp.setBoardLabel(boardLabel(post.getBoard()));
        resp.setTitle(post.getTitle());
        resp.setContent(cleanContent);
        resp.setImages(post.getImages());
        resp.setAuthorId(userId);
        resp.setIsAnonymous(post.getIsAnonymous());
        resp.setIsMine(true);
        resp.setIsLiked(false);
        resp.setIsFavorited(false);
        resp.setLikeCount(0);
        resp.setCommentCount(0);
        resp.setFavoriteCount(0);
        resp.setViewCount(0);
        resp.setIsCrisis(post.getIsCrisis());
        resp.setStatus(0);
        resp.setCreatedAt(post.getCreatedAt());
        User user = userMapper.selectById(userId);
        if (Integer.valueOf(1).equals(post.getIsAnonymous())) {
            resp.setAuthorName("匿名用户");
        } else if (user != null) {
            resp.setAuthorName(user.getDisplayName());
            resp.setAvatar(user.getAvatar());
        }
        if (crisis) {
            resp.setCrisisTip(buildCrisisTip());
        }
        return resp;
    }

    /**
     * 删除帖子（软删除 status=1），仅作者本人或管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId) && !CurrentUserUtils.isAdmin()) {
            throw new BusinessException("只能删除自己的帖子");
        }
        post.setStatus(CommunityPostStatus.DELETED.getCode());
        postMapper.updateById(post);
        // 软删帖触发热门榜 + 详情缓存延时双删
        evictPostCacheDoubleDelete(postId);
        log.info("删除社区帖子: postId={}, operator={}, asAdmin={}", postId, userId, CurrentUserUtils.isAdmin());
    }

    // ==================== 点赞 / 收藏 ====================

    /**
     * 点赞 toggle：写 community_like（唯一键防重）+ post.like_count 原子 ±1
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleLike(Long postId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != CommunityPostStatus.NORMAL.getCode()) {
            throw new BusinessException("帖子不存在或已下架");
        }
        CommunityLike existing = likeMapper.selectOne(new LambdaQueryWrapper<CommunityLike>()
                .eq(CommunityLike::getUserId, userId).eq(CommunityLike::getPostId, postId));
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                    .eq(CommunityPost::getId, postId).setSql("like_count = GREATEST(like_count - 1, 0)"));
            // like_count 变更触发详情 + 热门榜延时双删
            evictPostCacheDoubleDelete(postId);
            return false;
        }
        likeMapper.insert(CommunityLike.builder().userId(userId).postId(postId).createdAt(LocalDateTime.now()).build());
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId).setSql("like_count = like_count + 1"));
        // like_count 变更触发详情 + 热门榜延时双删
        evictPostCacheDoubleDelete(postId);
        return true;
    }

    /**
     * 收藏 toggle：写 community_favorite（唯一键防重）+ post.favorite_count 原子 ±1
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleFavorite(Long postId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != CommunityPostStatus.NORMAL.getCode()) {
            throw new BusinessException("帖子不存在或已下架");
        }
        CommunityFavorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<CommunityFavorite>()
                .eq(CommunityFavorite::getUserId, userId).eq(CommunityFavorite::getPostId, postId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                    .eq(CommunityPost::getId, postId).setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
            // favorite_count 变更触发详情 + 热门榜延时双删
            evictPostCacheDoubleDelete(postId);
            return false;
        }
        favoriteMapper.insert(CommunityFavorite.builder().userId(userId).postId(postId).createdAt(LocalDateTime.now()).build());
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId).setSql("favorite_count = favorite_count + 1"));
        // favorite_count 变更触发详情 + 热门榜延时双删
        evictPostCacheDoubleDelete(postId);
        return true;
    }

    // ==================== 评论 ====================

    /**
     * 评论分页：顶层评论分页 + 楼中楼一次查出后内存按 parentId 分组（禁止 N+1）
     */
    public Map<String, Object> listComments(Long postId, Long page, Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean showReal = CurrentUserUtils.isAdmin();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null || (post.getStatus() != null && post.getStatus() != CommunityPostStatus.NORMAL.getCode()
                && !post.getAuthorId().equals(userId) && !showReal)) {
            throw new BusinessException("帖子不存在或已下架");
        }
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 50L) : 10L;

        Page<CommentResponseDTO> p = new Page<>(cur, sz);
        IPage<CommentResponseDTO> result = commentMapper.selectTopCommentPage(p, postId, userId, showReal ? 1 : 0);
        List<CommentResponseDTO> tops = result.getRecords();

        // 楼中楼一次全量查出，内存分组
        List<CommentResponseDTO> replies = commentMapper.selectReplies(postId, userId, showReal ? 1 : 0);
        Map<Long, List<CommentResponseDTO>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(CommentResponseDTO::getParentId));
        tops.forEach(t -> t.setReplies(replyMap.getOrDefault(t.getId(), List.of())));

        return Map.of("records", tops, "total", result.getTotal());
    }

    /**
     * 发表评论 / 楼中楼回复（parentId 限一层）：敏感词拦截 + 危机词提示 + comment_count 原子 +1
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentResponseDTO createComment(Long postId, CommentCreateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != CommunityPostStatus.NORMAL.getCode()) {
            throw new BusinessException("帖子不存在或已下架");
        }
        // 楼中楼限一层：父评论必须存在、属于本帖、且本身是顶层评论
        if (dto.getParentId() != null) {
            CommunityComment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null || !postId.equals(parent.getPostId()) || parent.getParentId() != null) {
                throw new BusinessException("回复的评论不存在");
            }
        }
        sensitiveWordFilter.checkSensitive(dto.getContent());
        boolean crisis = sensitiveWordFilter.findCrisisWord(dto.getContent()) != null;

        CommunityComment comment = CommunityComment.builder()
                .postId(postId)
                .parentId(dto.getParentId())
                .authorId(userId)
                .content(dto.getContent().trim())
                .isAnonymous(Integer.valueOf(1).equals(dto.getIsAnonymous()) ? 1 : 0)
                .status(0)
                .createdAt(LocalDateTime.now())
                .build();
        commentMapper.insert(comment);
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId).setSql("comment_count = comment_count + 1"));
        // comment_count 变更触发详情 + 热门榜延时双删
        evictPostCacheDoubleDelete(postId);
        log.info("发表社区评论: userId={}, postId={}, commentId={}, crisis={}", userId, postId, comment.getId(), crisis);

        CommentResponseDTO resp = new CommentResponseDTO();
        resp.setId(comment.getId());
        resp.setPostId(postId);
        resp.setParentId(dto.getParentId());
        resp.setAuthorId(userId);
        resp.setContent(comment.getContent());
        resp.setIsAnonymous(comment.getIsAnonymous());
        resp.setIsMine(true);
        resp.setCreatedAt(comment.getCreatedAt());
        resp.setReplies(List.of());
        User user = userMapper.selectById(userId);
        if (Integer.valueOf(1).equals(comment.getIsAnonymous())) {
            resp.setAuthorName("匿名用户");
        } else if (user != null) {
            resp.setAuthorName(user.getDisplayName());
            resp.setAvatar(user.getAvatar());
        }
        if (crisis) {
            resp.setCrisisTip(buildCrisisTip());
        }
        return resp;
    }

    /**
     * 删除评论（软删除 status=1），仅作者本人或管理员；comment_count 原子 -1
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getAuthorId().equals(userId) && !CurrentUserUtils.isAdmin()) {
            throw new BusinessException("只能删除自己的评论");
        }
        comment.setStatus(1);
        commentMapper.updateById(comment);
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, comment.getPostId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
        // comment_count 变更触发详情 + 热门榜延时双删
        evictPostCacheDoubleDelete(comment.getPostId());
        log.info("删除社区评论: commentId={}, operator={}, asAdmin={}", commentId, userId, CurrentUserUtils.isAdmin());
    }

    // ==================== 举报 ====================

    /**
     * 举报帖子/评论：同人同目标 24h 内限 1 次防刷；帖子被举报后 status 置 2（举报待审）
     */
    @Transactional(rollbackFor = Exception.class)
    public void report(ReportCreateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CommunityReportTarget target = CommunityReportTarget.fromCode(dto.getTargetType());
        if (target == null) {
            throw new BusinessException("举报对象类型不正确");
        }
        // 校验举报对象存在
        if (target == CommunityReportTarget.POST) {
            CommunityPost post = postMapper.selectById(dto.getTargetId());
            if (post == null || post.getStatus() == null
                    || post.getStatus() == CommunityPostStatus.DELETED.getCode()
                    || post.getStatus() == CommunityPostStatus.TAKEN_DOWN.getCode()) {
                throw new BusinessException("举报的帖子不存在");
            }
        } else {
            CommunityComment comment = commentMapper.selectById(dto.getTargetId());
            if (comment == null || comment.getStatus() == null || comment.getStatus() != 0) {
                throw new BusinessException("举报的评论不存在");
            }
        }
        // 防刷：同人同目标 24h 内已有待处理/已处理举报则拒绝
        Long recent = reportMapper.selectCount(new LambdaQueryWrapper<CommunityReport>()
                .eq(CommunityReport::getReporterId, userId)
                .eq(CommunityReport::getTargetType, dto.getTargetType())
                .eq(CommunityReport::getTargetId, dto.getTargetId())
                .ge(CommunityReport::getCreatedAt, LocalDateTime.now().minusHours(24)));
        if (recent > 0) {
            throw new BusinessException("你已举报过该内容，请勿重复举报，管理员会尽快处理");
        }

        CommunityReport report = CommunityReport.builder()
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .reporterId(userId)
                .reason(dto.getReason().trim())
                .status(CommunityReportStatus.PENDING.getCode())
                .createdAt(LocalDateTime.now())
                .build();
        reportMapper.insert(report);

        // 被举报帖置为「举报待审」（评论举报不改变帖子状态，处理时一并处置）
        if (target == CommunityReportTarget.POST) {
            postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                    .eq(CommunityPost::getId, dto.getTargetId())
                    .eq(CommunityPost::getStatus, CommunityPostStatus.NORMAL.getCode())
                    .set(CommunityPost::getStatus, CommunityPostStatus.PENDING_REVIEW.getCode()));
            // 帖子状态变更触发详情 + 热门榜延时双删
            evictPostCacheDoubleDelete(dto.getTargetId());
        }
        log.info("社区举报: reporter={}, targetType={}, targetId={}", userId, dto.getTargetType(), dto.getTargetId());
    }

    // ==================== 搜索 ====================

    /**
     * 关键词搜索（标题前缀 + 正文 LIKE），不缓存；关键词 ≤50 字
     */
    public Map<String, Object> search(String keyword, Long page, Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean showReal = CurrentUserUtils.isAdmin();
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException("请输入搜索关键词");
        }
        String kw = keyword.trim();
        if (kw.length() > 50) {
            throw new BusinessException("关键词不能超过50个字符");
        }
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 20L) : 10L;

        Page<PostResponseDTO> p = new Page<>(cur, sz);
        IPage<PostResponseDTO> result = postMapper.searchPosts(p, kw, userId, showReal ? 1 : 0);
        polishList(result.getRecords());
        return Map.of("records", result.getRecords(), "total", result.getTotal());
    }

    // ==================== 我的 ====================

    /**
     * 我的社区：我的帖子（含待审/下架状态）/ 我的评论 / 我的收藏
     */
    public CommunityMineResponseDTO listMine() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        User me = userMapper.selectById(userId);

        // 我的帖子（本人视角，作者真实身份；含全部状态）
        List<CommunityPost> posts = postMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getAuthorId, userId)
                .ne(CommunityPost::getStatus, CommunityPostStatus.DELETED.getCode())
                .orderByDesc(CommunityPost::getCreatedAt)
                .last("LIMIT " + MINE_LIMIT));
        List<PostResponseDTO> postDTOs = posts.stream().map(p -> {
            PostResponseDTO dto = new PostResponseDTO();
            dto.setId(p.getId());
            dto.setBoard(p.getBoard());
            dto.setBoardLabel(boardLabel(p.getBoard()));
            dto.setTitle(p.getTitle());
            dto.setSummary(cleanSummary(p.getContent()));
            dto.setImages(p.getImages());
            dto.setAuthorId(userId);
            dto.setIsAnonymous(p.getIsAnonymous());
            dto.setAuthorName(me != null ? me.getDisplayName() : null);
            dto.setAvatar(me != null ? me.getAvatar() : null);
            dto.setIsMine(true);
            dto.setLikeCount(p.getLikeCount());
            dto.setCommentCount(p.getCommentCount());
            dto.setFavoriteCount(p.getFavoriteCount());
            dto.setViewCount(p.getViewCount());
            dto.setIsCrisis(p.getIsCrisis());
            dto.setStatus(p.getStatus());
            dto.setCreatedAt(p.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        // 我的评论
        List<CommentResponseDTO> comments = commentMapper.selectMyComments(userId);

        // 我的收藏
        Page<PostResponseDTO> favPage = new Page<>(1, MINE_LIMIT);
        IPage<PostResponseDTO> favResult = postMapper.selectFavoritePage(favPage, userId);
        polishList(favResult.getRecords());

        CommunityMineResponseDTO result = new CommunityMineResponseDTO();
        result.setPosts(postDTOs);
        result.setComments(comments);
        result.setFavorites(favResult.getRecords());
        return result;
    }

    // ==================== 板块 ====================

    /**
     * 板块列表（静态枚举，缓存 1 天）
     */
    public List<Map<String, String>> listBoards() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.COMMUNITY_BOARDS_KEY);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<List<Map<String, String>>>() {
                });
            }
        } catch (Exception e) {
            log.warn("板块缓存读取异常: {}", e.getMessage());
        }
        List<Map<String, String>> boards = new ArrayList<>();
        for (CommunityBoard b : CommunityBoard.values()) {
            boards.add(Map.of("code", b.getCode(), "label", b.getLabel()));
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.COMMUNITY_BOARDS_KEY,
                    objectMapper.writeValueAsString(boards),
                    RedisConstants.COMMUNITY_BOARDS_TTL_DAYS,
                    TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.warn("板块缓存写入失败（fail-open）: {}", e.getMessage());
        }
        return boards;
    }

    // ==================== 后台：举报管理 ====================

    /**
     * 举报列表分页（管理员）
     */
    public Map<String, Object> pageReports(Integer status, Long page, Long size) {
        ensureAdmin();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 50L) : 10L;
        Page<ReportResponseDTO> p = new Page<>(cur, sz);
        IPage<ReportResponseDTO> result = reportMapper.selectReportPage(p, status);
        return Map.of("records", result.getRecords(), "total", result.getTotal());
    }

    /**
     * 处理举报（管理员）：DOWN-下架内容（帖子 status=3 / 评论 status=1）；IGNORE-忽略（举报 status=2，
     * 若帖子为举报待审则恢复正常）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long reportId, String action) {
        ensureAdmin();
        CommunityReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        if (report.getStatus() != null && report.getStatus() != CommunityReportStatus.PENDING.getCode()) {
            throw new BusinessException("该举报已处理");
        }
        boolean down = "DOWN".equals(action);
        report.setStatus(down ? CommunityReportStatus.TAKEN_DOWN.getCode() : CommunityReportStatus.IGNORED.getCode());
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);

        CommunityReportTarget target = CommunityReportTarget.fromCode(report.getTargetType());
        if (down) {
            if (target == CommunityReportTarget.POST) {
                postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                        .eq(CommunityPost::getId, report.getTargetId())
                        .set(CommunityPost::getStatus, CommunityPostStatus.TAKEN_DOWN.getCode()));
                // 帖子下架触发详情 + 热门榜延时双删
                evictPostCacheDoubleDelete(report.getTargetId());
            } else if (target == CommunityReportTarget.COMMENT) {
                CommunityComment comment = commentMapper.selectById(report.getTargetId());
                if (comment != null && comment.getStatus() != null && comment.getStatus() == 0) {
                    comment.setStatus(1);
                    commentMapper.updateById(comment);
                    postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                            .eq(CommunityPost::getId, comment.getPostId())
                            .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
                    // comment_count 变更触发详情 + 热门榜延时双删
                    evictPostCacheDoubleDelete(comment.getPostId());
                }
            }
        } else {
            // 忽略：若帖子仍处于「举报待审」且无其他待处理举报，恢复正常
            if (target == CommunityReportTarget.POST) {
                Long pending = reportMapper.selectCount(new LambdaQueryWrapper<CommunityReport>()
                        .eq(CommunityReport::getTargetType, CommunityReportTarget.POST.getCode())
                        .eq(CommunityReport::getTargetId, report.getTargetId())
                        .eq(CommunityReport::getStatus, CommunityReportStatus.PENDING.getCode()));
                if (pending == 0) {
                    postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                            .eq(CommunityPost::getId, report.getTargetId())
                            .eq(CommunityPost::getStatus, CommunityPostStatus.PENDING_REVIEW.getCode())
                            .set(CommunityPost::getStatus, CommunityPostStatus.NORMAL.getCode()));
                    // 帖子恢复正常触发详情 + 热门榜延时双删
                    evictPostCacheDoubleDelete(report.getTargetId());
                }
            }
        }
        log.info("处理社区举报: reportId={}, action={}, operator=admin", reportId, action);
    }

    // ==================== 定时兜底 ====================

    /**
     * 每 5 分钟重建热门榜缓存（延时双删失效/丢任务时的最终一致性兜底）
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 30 * 1000L)
    public void scheduledRebuildHot() {
        try {
            rebuildHotCache();
        } catch (Exception e) {
            log.warn("定时重建热门榜失败: {}", e.getMessage());
        }
    }

    /**
     * 每小时扫描社区命名空间，清理未设置 TTL 的残留 key（防御性，正常 key 均带过期时间）
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void cleanPersistentKeys() {
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(RedisConstants.ROOT_NAMESPACE + ":" + RedisConstants.MODULE_COMMUNITY + ":*")
                    .count(200).build();
            List<String> removed = new ArrayList<>();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    Long ttl = stringRedisTemplate.getExpire(key);
                    // ttl == -1 表示永久存在（异常残留），删除交由下次读重建
                    if (ttl != null && ttl == -1) {
                        stringRedisTemplate.delete(key);
                        removed.add(key);
                    }
                }
            }
            if (!removed.isEmpty()) {
                log.info("清理社区无TTL残留key: {}", removed);
            }
        } catch (Exception e) {
            log.warn("清理社区残留key异常: {}", e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private void ensureAdmin() {
        if (!CurrentUserUtils.isAdmin()) {
            throw new BusinessException("A0301", "无管理员权限", null);
        }
    }

    /**
     * 列表后处理：板块中文名 + 摘要纯文本 + 用户互动状态
     */
    private void polishList(List<PostResponseDTO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 定时任务/冷启动场景无 HTTP 上下文，getCurrentUserId 返回 null 时跳过互动状态补齐
        Long userId = null;
        try {
            userId = CurrentUserUtils.getCurrentUserId();
        } catch (Exception ignored) {
            // 非请求上下文下获取不到用户，正常跳过
        }
        list.forEach(dto -> {
            dto.setBoardLabel(boardLabel(dto.getBoard()));
            dto.setSummary(cleanSummary(dto.getSummary()));
        });
        if (userId != null) {
            fillUserReactions(list, userId);
        }
    }

    /**
     * 批量补齐当前用户对帖子的点赞/收藏/本人标记（IN 查询，禁止 N+1）
     */
    private void fillUserReactions(List<PostResponseDTO> list, Long userId) {
        if (list == null || list.isEmpty() || userId == null) {
            return;
        }
        List<Long> ids = list.stream().map(PostResponseDTO::getId).toList();
        Set<Long> liked = ids.isEmpty() ? new HashSet<>()
                : new HashSet<>(likeMapper.selectLikedPostIds(userId, ids));
        Set<Long> favorited = ids.isEmpty() ? new HashSet<>()
                : new HashSet<>(favoriteMapper.selectFavoritedPostIds(userId, ids));
        list.forEach(dto -> {
            dto.setIsLiked(liked.contains(dto.getId()));
            dto.setIsFavorited(favorited.contains(dto.getId()));
            if (dto.getIsMine() == null) {
                dto.setIsMine(dto.getAuthorId() != null && dto.getAuthorId().equals(userId));
            }
        });
    }

    /**
     * HTML 片段 → 纯文本摘要（去标签、反转义、压缩空白、截 100 字）
     */
    private String cleanSummary(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        String text = HtmlUtil.unescape(HtmlUtil.cleanHtmlTag(rawHtml)).replaceAll("\\s+", " ").trim();
        return text.length() > 100 ? text.substring(0, 100) : text;
    }

    private String boardLabel(String code) {
        CommunityBoard board = CommunityBoard.fromCode(code);
        return board != null ? board.getLabel() : code;
    }

    /**
     * 危机干预提示文案：共情、不评判，提供全国心理援助热线 12356
     */
    private Map<String, String> buildCrisisTip() {
        return Map.of(
                "title", "我们很关心你 🤍",
                "content", "看到你正在经历一段很艰难的时光，愿意把感受写下来，本身就是一种勇气。"
                        + "如果你出现了伤害自己的念头，请立刻联系身边可信任的人陪伴你，"
                        + "或拨打全国心理援助热线 12356，专业的人会 24 小时倾听你、帮助你。",
                "hotline", "12356",
                "suggestion", "也建议你尽早寻求学校心理老师、心理咨询师或精神科医生的专业支持，你不必独自承受。"
        );
    }
}
