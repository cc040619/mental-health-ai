package com.cc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.common.Result;
import com.cc.dto.request.CommentCreateDTO;
import com.cc.dto.request.PostCreateDTO;
import com.cc.dto.request.ReportCreateDTO;
import com.cc.dto.response.CommentResponseDTO;
import com.cc.dto.response.CommunityMineResponseDTO;
import com.cc.dto.response.PostResponseDTO;
import com.cc.entity.CommunityPost;
import com.cc.mapper.CommunityPostMapper;
import com.cc.service.CommunityService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 社区论坛控制器（用户端）
 *
 * <p>前缀 /api/community，全部接口需登录（SecurityConfig 默认拦截，PUBLIC_PATHS 不含本路径）</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Resource
    private CommunityService communityService;
    @Resource
    private CommunityPostMapper postMapper;

    // 信息流（板块筛选 + recommend/latest/hot，分页 size≤20）
    @GetMapping("/posts")
    public Result<Map<String, Object>> listPosts(@RequestParam(required = false) String board,
                                                 @RequestParam(required = false, defaultValue = "latest") String tab,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long size) {
        return Result.ok(communityService.listPosts(board, tab, page, size));
    }

    // 板块列表（前端板块导航）
    @GetMapping("/boards")
    public Result<List<Map<String, String>>> listBoards() {
        return Result.ok(communityService.listBoards());
    }

    // 帖子详情（浏览数 +1，返回正文/作者/互动状态/危机提示）
    @GetMapping("/post/{id}")
    public Result<PostResponseDTO> getPost(@PathVariable Long id) {
        return Result.ok(communityService.getPostDetail(id));
    }

    // 发帖（敏感词校验 + HTML 清洗 + 危机词标记）
    @PostMapping("/post")
    public Result<PostResponseDTO> createPost(@Valid @RequestBody PostCreateDTO dto) {
        return Result.ok(communityService.createPost(dto));
    }

    // 删除自己的帖子（软删除，管理员可删任意帖）
    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        communityService.deletePost(id);
        return Result.ok();
    }

    // 点赞 toggle，返回最新状态 true=已点赞
    @PostMapping("/post/{id}/like")
    public Result<Boolean> toggleLike(@PathVariable Long id) {
        return Result.ok(communityService.toggleLike(id));
    }

    // 收藏 toggle，返回最新状态 true=已收藏
    @PostMapping("/post/{id}/favorite")
    public Result<Boolean> toggleFavorite(@PathVariable Long id) {
        return Result.ok(communityService.toggleFavorite(id));
    }

    // 评论分页（顶层 + 楼中楼分组）
    @GetMapping("/post/{id}/comments")
    public Result<Map<String, Object>> listComments(@PathVariable Long id,
                                                    @RequestParam(required = false) Long page,
                                                    @RequestParam(required = false) Long size) {
        return Result.ok(communityService.listComments(id, page, size));
    }

    // 发表评论 / 楼中楼回复（parentId 限一层）
    @PostMapping("/post/{id}/comment")
    public Result<CommentResponseDTO> createComment(@PathVariable Long id,
                                                    @Valid @RequestBody CommentCreateDTO dto) {
        return Result.ok(communityService.createComment(id, dto));
    }

    // 删除自己的评论（软删除，管理员可删任意评论）
    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        communityService.deleteComment(id);
        return Result.ok();
    }

    // 举报帖子 / 评论（同人同目标 24h 限 1 次）
    @PostMapping("/report")
    public Result<Void> report(@Valid @RequestBody ReportCreateDTO dto) {
        communityService.report(dto);
        return Result.ok();
    }

    // 搜索（标题前缀 + 正文 LIKE，关键词 ≤50 字）
    @GetMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam String keyword,
                                              @RequestParam(required = false) Long page,
                                              @RequestParam(required = false) Long size) {
        return Result.ok(communityService.search(keyword, page, size));
    }

    // 我的社区（我的帖子 / 我的评论 / 我的收藏）
    @GetMapping("/mine")
    public Result<CommunityMineResponseDTO> listMine() {
        return Result.ok(communityService.listMine());
    }

    /**
     * 危机干预引导位：当前用户发布过危机帖时，返回最近一条危机帖与系统干预提示；
     * 无则 hasCrisis=false。语气克制不评判。
     */
    @GetMapping("/daily/practice")
    public Result<Map<String, Object>> crisisNotice() {
        Long userId = com.cc.util.CurrentUserUtils.getCurrentUserId();
        CommunityPost crisisPost = postMapper.selectOne(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getAuthorId, userId)
                .eq(CommunityPost::getIsCrisis, 1)
                .eq(CommunityPost::getStatus, 0)
                .orderByDesc(CommunityPost::getCreatedAt)
                .last("LIMIT 1"));
        Map<String, Object> data = new HashMap<>();
        data.put("hasCrisis", crisisPost != null);
        if (crisisPost != null) {
            data.put("postId", crisisPost.getId());
            data.put("title", crisisPost.getTitle());
            data.put("tip", Map.of(
                    "title", "我们一直在这里 🤍",
                    "content", "谢谢你愿意把此刻的感受写下来。如果痛苦还在，请记得你不是一个人，"
                            + "拨打全国心理援助热线 12356，随时有人愿意倾听你。",
                    "hotline", "12356"
            ));
        }
        return Result.ok(data);
    }
}
